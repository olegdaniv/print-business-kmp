package com.printbusinesskmp.auth

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AllowlistServiceTest {

    @Test
    fun `parseAllowedEmails trims lowers and deduplicates emails`() {
        val parsed = EnvAllowlistService.parseAllowedEmails(
            " Admin@Example.com, manager@example.com ,,admin@example.com "
        )

        assertEquals(
            setOf("admin@example.com", "manager@example.com"),
            parsed
        )
    }

    @Test
    fun `isAllowed checks normalized email`() {
        val allowlist = EnvAllowlistService(
            allowedEmails = setOf("admin@example.com")
        )

        runBlocking {
            assertTrue(allowlist.isAllowed("ADMIN@example.com"))
            assertFalse(allowlist.isAllowed("blocked@example.com"))
        }
    }
}
