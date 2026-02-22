package com.printbusinesskmp.desktop.update

private val SEMVER_REGEX = Regex(
    pattern = "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?$"
)

internal data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String> = emptyList()
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        val majorDiff = major.compareTo(other.major)
        if (majorDiff != 0) return majorDiff

        val minorDiff = minor.compareTo(other.minor)
        if (minorDiff != 0) return minorDiff

        val patchDiff = patch.compareTo(other.patch)
        if (patchDiff != 0) return patchDiff

        if (preRelease.isEmpty() && other.preRelease.isEmpty()) return 0
        if (preRelease.isEmpty()) return 1
        if (other.preRelease.isEmpty()) return -1

        val max = maxOf(preRelease.size, other.preRelease.size)
        for (index in 0 until max) {
            val left = preRelease.getOrNull(index)
            val right = other.preRelease.getOrNull(index)

            if (left == null) return -1
            if (right == null) return 1

            val leftNumber = left.toIntOrNull()
            val rightNumber = right.toIntOrNull()
            val identifierDiff = when {
                leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                leftNumber != null -> -1
                rightNumber != null -> 1
                else -> left.compareTo(right)
            }
            if (identifierDiff != 0) return identifierDiff
        }

        return 0
    }

    companion object {
        fun parse(value: String): SemVer {
            val trimmed = value.trim()
            val match = SEMVER_REGEX.matchEntire(trimmed)
                ?: throw IllegalArgumentException("Invalid SemVer: $value")

            val preRelease = match.groupValues[4]
                .takeIf { it.isNotEmpty() }
                ?.split(".")
                ?: emptyList()

            return SemVer(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                preRelease = preRelease
            )
        }
    }
}
