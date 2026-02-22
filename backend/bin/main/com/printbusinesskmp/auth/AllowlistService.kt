package com.printbusinesskmp.auth

import com.printbusinesskmp.repository.AllowedEmailRecord
import com.printbusinesskmp.repository.AllowedEmailRepository
import java.time.Instant

interface AllowlistService {
    suspend fun isAllowed(email: String): Boolean
    suspend fun addEmail(email: String, note: String? = null): AllowedEmailEntry
    suspend fun removeEmail(email: String): Boolean
    suspend fun listEmails(): List<AllowedEmailEntry>
}

data class AllowedEmailEntry(
    val email: String,
    val createdAt: Instant,
    val note: String? = null
)

class DatabaseAllowlistService(
    private val repository: AllowedEmailRepository
) : AllowlistService {

    override suspend fun isAllowed(email: String): Boolean {
        val normalizedEmail = normalizeEmail(email)
        if (normalizedEmail.isEmpty()) return false
        return repository.isAllowed(normalizedEmail)
    }

    override suspend fun addEmail(email: String, note: String?): AllowedEmailEntry {
        val normalizedEmail = validateEmailForWrite(email)
        val normalizedNote = note?.trim()?.takeIf { it.isNotEmpty() }

        if (repository.findByEmail(normalizedEmail) != null) {
            throw IllegalStateException("Email '$normalizedEmail' is already in the allowlist")
        }

        return repository.addEmail(normalizedEmail, normalizedNote).toEntry()
    }

    override suspend fun removeEmail(email: String): Boolean {
        val normalizedEmail = validateEmailForWrite(email)
        return repository.removeEmail(normalizedEmail)
    }

    override suspend fun listEmails(): List<AllowedEmailEntry> {
        return repository.listEmails().map { it.toEntry() }
    }

    private fun validateEmailForWrite(email: String): String {
        val normalizedEmail = normalizeEmail(email)
        require(normalizedEmail.isNotEmpty()) { "Email is required" }
        require(EMAIL_REGEX.matches(normalizedEmail)) { "Email '$normalizedEmail' is not valid" }
        return normalizedEmail
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

class DbFirstAllowlistService(
    private val dbAllowlistService: DatabaseAllowlistService,
    private val envAllowlistService: EnvAllowlistService
) : AllowlistService {

    override suspend fun isAllowed(email: String): Boolean {
        return try {
            dbAllowlistService.isAllowed(email)
        } catch (error: Exception) {
            System.err.println(
                "Warning: allowlist DB check failed (${error.message}). Falling back to ALLOWED_EMAILS env var."
            )
            envAllowlistService.isAllowed(email)
        }
    }

    override suspend fun addEmail(email: String, note: String?): AllowedEmailEntry {
        return dbAllowlistService.addEmail(email, note)
    }

    override suspend fun removeEmail(email: String): Boolean {
        return dbAllowlistService.removeEmail(email)
    }

    override suspend fun listEmails(): List<AllowedEmailEntry> {
        return dbAllowlistService.listEmails()
    }
}

class EnvAllowlistService(
    private val allowedEmails: Set<String>
) : AllowlistService {

    override suspend fun isAllowed(email: String): Boolean {
        return normalizeEmail(email) in allowedEmails
    }

    override suspend fun addEmail(email: String, note: String?): AllowedEmailEntry {
        throw UnsupportedOperationException("Cannot add allowlist email when only env fallback is available")
    }

    override suspend fun removeEmail(email: String): Boolean {
        throw UnsupportedOperationException("Cannot remove allowlist email when only env fallback is available")
    }

    override suspend fun listEmails(): List<AllowedEmailEntry> {
        return allowedEmails.sorted().map { email ->
            AllowedEmailEntry(
                email = email,
                createdAt = Instant.EPOCH,
                note = "env-fallback"
            )
        }
    }

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): EnvAllowlistService {
            return EnvAllowlistService(parseAllowedEmails(env["ALLOWED_EMAILS"]))
        }

        internal fun parseAllowedEmails(rawAllowedEmails: String?): Set<String> {
            return rawAllowedEmails
                .orEmpty()
                .split(',')
                .map(::normalizeEmail)
                .filter { it.isNotEmpty() }
                .toSet()
        }
    }
}

private fun normalizeEmail(value: String): String = value.trim().lowercase()

private fun AllowedEmailRecord.toEntry(): AllowedEmailEntry {
    return AllowedEmailEntry(
        email = email,
        createdAt = createdAt,
        note = note
    )
}
