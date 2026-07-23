package com.zhousl.aether.data

import com.zhousl.aether.data.pi.RuntimeHostToolExecutor
import com.zhousl.aether.runtime.MultiplatformLocalRuntime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class SharedInstalledSkill(
    val id: String,
    val name: String,
    val description: String,
    val guestPath: String,
)

class SharedSkillManager(
    private val runtime: MultiplatformLocalRuntime,
) {
    private val executor = RuntimeHostToolExecutor(runtime)
    private val skillsRoot = "${runtime.homeDirectory.trimEnd('/')}/.aether/skills"

    suspend fun list(): List<SharedInstalledSkill> {
        runtime.fileSystem.createDirectories(skillsRoot)
        val result = bash("find ${quote(skillsRoot)} -mindepth 2 -maxdepth 2 -name SKILL.md -type f -print | sort")
        if (result.isError) return emptyList()
        return buildList {
            for (path in result.stdout().lineSequence().map(String::trim).filter(String::isNotBlank)) {
                val skill = runCatching {
                val markdown = runtime.fileSystem.read(path).decodeToString()
                val metadata = parseSkillMetadata(markdown)
                SharedInstalledSkill(
                    id = path.substringBeforeLast('/').substringAfterLast('/'),
                    name = metadata.first.ifBlank { path.substringBeforeLast('/').substringAfterLast('/') },
                    description = metadata.second,
                    guestPath = path.substringBeforeLast('/'),
                )
                }.getOrNull()
                if (skill != null) add(skill)
            }
        }
    }

    suspend fun installArchive(
        archivePath: String,
        preferredSubpath: String = "",
    ): SharedInstalledSkill {
        runtime.fileSystem.createDirectories(skillsRoot)
        val staging = "${runtime.homeDirectory}/.aether/skill-import-${platformRandomUuid()}"
        val normalizedSubpath = preferredSubpath.trim('/').also { path ->
            require(path.split('/').none { it == ".." }) { "Skill subpath must not contain '..'." }
        }
        val findSkillCommand = if (normalizedSubpath.isBlank()) {
            "find ${quote(staging)} -name SKILL.md -type f | head -1"
        } else {
            "find ${quote(staging)} -path ${quote("*/$normalizedSubpath/SKILL.md")} -type f | head -1"
        }
        val command = """
            set -eu
            command -v unzip >/dev/null 2>&1 || apk add --no-cache unzip >/dev/null
            mkdir -p ${quote(staging)}
            unzip -q ${quote(archivePath)} -d ${quote(staging)}
            skill_file=${'$'}($findSkillCommand)
            test -n "${'$'}skill_file"
            skill_root=${'$'}{skill_file%/SKILL.md}
            skill_id=${'$'}(basename "${'$'}skill_root" | tr -cs 'A-Za-z0-9._-' '-' | tr 'A-Z' 'a-z')
            test -n "${'$'}skill_id"
            rm -rf ${quote(skillsRoot)}/"${'$'}skill_id"
            cp -R "${'$'}skill_root" ${quote(skillsRoot)}/"${'$'}skill_id"
            rm -rf ${quote(staging)}
            printf '%s' "${'$'}skill_id"
        """.trimIndent()
        val result = bash(command)
        check(!result.isError) { result.errorText().ifBlank { "Unable to install Skill archive." } }
        val installedId = result.stdout().trim()
        return list().firstOrNull { it.id == installedId } ?: error("Installed Skill was not found.")
    }

    suspend fun installRemote(url: String): SharedInstalledSkill {
        require(url.startsWith("https://")) { "Skill URL must use HTTPS." }
        val archive = "${runtime.workspaceRoot.trimEnd('/')}/.aether-skill-${platformRandomUuid()}.zip"
        val source = resolveRemoteSkillSource(url)
        val result = bash(
            "command -v curl >/dev/null 2>&1 || apk add --no-cache curl >/dev/null; " +
                "curl -fL --max-time 90 ${quote(source.downloadUrl)} -o ${quote(archive)}"
        )
        check(!result.isError) { result.errorText().ifBlank { "Unable to download Skill." } }
        return try {
            installArchive(archive, source.subpath)
        } finally {
            runtime.fileSystem.remove(archive)
        }
    }

    suspend fun remove(skillId: String) {
        require(skillId.matches(Regex("[A-Za-z0-9._-]+"))) { "Invalid Skill ID." }
        runtime.fileSystem.remove("$skillsRoot/$skillId", recursive = true)
    }

    suspend fun buildPrompt(selectedIds: Set<String>): String {
        val prompts = mutableListOf<String>()
        for (skill in list().filter { it.id in selectedIds }) {
            val body = runtime.fileSystem.read("${skill.guestPath}/SKILL.md").decodeToString()
            prompts += "<skill name=\"${skill.name}\" root=\"${skill.guestPath}\">\n$body\n</skill>"
        }
        return prompts.joinToString("\n\n")
    }

    private suspend fun bash(command: String) = executor.execute(
        "bash",
        buildJsonObject {
            put("command", command)
            put("working_directory", runtime.workspaceRoot)
        },
    )
}

private fun parseSkillMetadata(markdown: String): Pair<String, String> {
    val header = markdown.lineSequence().drop(1).takeWhile { it.trim() != "---" }.toList()
    fun value(key: String): String = header.firstOrNull { it.trimStart().startsWith("$key:") }
        ?.substringAfter(':')?.trim()?.trim('"', '\'').orEmpty()
    return value("name") to value("description")
}

internal data class SharedSkillRemoteSource(
    val downloadUrl: String,
    val subpath: String = "",
)

internal fun resolveRemoteSkillSource(url: String): SharedSkillRemoteSource {
    val normalized = url.removeSuffix("/")
    val match = Regex("https://github\\.com/([^/]+)/([^/]+)(?:/tree/([^/]+)(?:/(.*))?)?").matchEntire(normalized)
        ?: return SharedSkillRemoteSource(normalized)
    val owner = match.groupValues[1]
    val repo = match.groupValues[2].removeSuffix(".git")
    val ref = match.groupValues[3].ifBlank { "HEAD" }
    return SharedSkillRemoteSource(
        downloadUrl = "https://github.com/$owner/$repo/archive/$ref.zip",
        subpath = match.groupValues[4].trim('/'),
    )
}

private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

private fun com.zhousl.aether.data.pi.SharedHostToolResult.payload(): JsonObject =
    Json.parseToJsonElement(outputJson).jsonObject

private fun com.zhousl.aether.data.pi.SharedHostToolResult.stdout(): String =
    payload()["stdout"]?.jsonPrimitive?.content.orEmpty()

private fun com.zhousl.aether.data.pi.SharedHostToolResult.errorText(): String =
    sequenceOf(
        payload()["stderr"]?.jsonPrimitive?.content.orEmpty(),
        payload()["error"]?.jsonPrimitive?.content.orEmpty(),
    ).firstOrNull { it.isNotBlank() }.orEmpty()
