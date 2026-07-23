package com.zhousl.aether.data

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedSkillManagerTest {
    @Test
    fun resolvesGitHubRepositoryRoot() {
        assertEquals(
            SharedSkillRemoteSource("https://github.com/openai/example/archive/HEAD.zip"),
            resolveRemoteSkillSource("https://github.com/openai/example"),
        )
    }

    @Test
    fun preservesGitHubTreeSubpath() {
        assertEquals(
            SharedSkillRemoteSource(
                downloadUrl = "https://github.com/openai/example/archive/main.zip",
                subpath = "skills/review",
            ),
            resolveRemoteSkillSource("https://github.com/openai/example/tree/main/skills/review"),
        )
    }

    @Test
    fun leavesDirectArchiveUrlUntouched() {
        val url = "https://example.com/aether-skill.zip"
        assertEquals(SharedSkillRemoteSource(url), resolveRemoteSkillSource(url))
    }
}
