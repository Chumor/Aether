package com.zhousl.aether.data.pi

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedMcpManagerTest {
    @Test
    fun serverConfigurationRoundTrips() {
        val servers = listOf(
            SharedMcpServerConfig(
                id = "stdio-id",
                name = "Local Files",
                transport = SharedMcpTransport.Stdio,
                command = "/usr/bin/node",
                arguments = listOf("server.mjs", "--stdio"),
                enabled = false,
            ),
            SharedMcpServerConfig(
                id = "http-id",
                name = "Remote Search",
                transport = SharedMcpTransport.Http,
                url = "https://example.com/mcp",
            ),
        )

        assertEquals(servers, parseSharedMcpServers(serializeSharedMcpServers(servers)))
    }

    @Test
    fun exposedToolNameIsStableAndSafe() {
        assertEquals(
            "mcp__local_files__search_docs",
            sharedMcpToolName("Local Files!", "search.docs"),
        )
        assertEquals("mcp__unnamed__unnamed", sharedMcpToolName("---", ""))
    }
}
