package com.zhousl.aether.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedChatBranchTest {
    @Test
    fun completedOnboardingRestoresChatEvenWhenProviderWasSkipped() {
        assertTrue(shouldRestoreSharedChat(onboardingCompletedVersion = 1))
        assertFalse(shouldRestoreSharedChat(onboardingCompletedVersion = 0))
    }

    @Test
    fun selectsOneAssistantReplyWithoutDroppingAlternatives() {
        val group = "user-1"
        val messages = listOf(
            SharedChatMessage(id = group, text = "Question", fromUser = true),
            SharedChatMessage(
                id = "reply-1",
                text = "First",
                fromUser = false,
                responseGroupId = group,
                isActiveBranch = true,
            ),
            SharedChatMessage(
                id = "reply-2",
                text = "Second",
                fromUser = false,
                responseGroupId = group,
                isActiveBranch = false,
            ),
        )
        val selected = messages.selectSharedResponseBranch(group, 1)
        assertEquals(3, selected.size)
        assertFalse(selected[1].isActiveBranch)
        assertTrue(selected[2].isActiveBranch)
        assertEquals(2, selected[2].branchCount)
        assertEquals(1, selected[2].branchIndex)
    }

    @Test
    fun backgroundExpirationPreservesPartialOutputAndStopsTools() {
        val partial = SharedChatMessage(
            text = "Partial answer",
            fromUser = false,
            isStreaming = true,
            tools = listOf(
                SharedChatToolInvocation(
                    id = "tool",
                    name = "bash",
                    summary = "Running",
                    isRunning = true,
                )
            ),
        ).interruptedByBackgroundExpiration()

        assertEquals("Partial answer", partial.text)
        assertFalse(partial.isStreaming)
        assertEquals("Interrupted", partial.status)
        assertFalse(partial.tools.single().isRunning)
        assertTrue(partial.tools.single().isError)
    }
}
