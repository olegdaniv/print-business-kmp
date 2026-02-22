package com.printbusinesskmp.platform

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDataPathsTest {

    @Test
    fun `uses override when PRINTBUSINESS_DATA_ROOT is provided`() {
        val resolved = AppDataPaths.resolveDataRoot(
            osName = "Windows 11",
            homeDir = "C:\\Users\\alice",
            dataRootOverride = "D:\\PrintBusinessData",
            localAppData = "C:\\Users\\alice\\AppData\\Local"
        )

        assertEquals(Paths.get("D:\\PrintBusinessData").toAbsolutePath().normalize(), resolved)
    }

    @Test
    fun `uses LOCALAPPDATA on windows by default`() {
        val resolved = AppDataPaths.resolveDataRoot(
            osName = "Windows 11",
            homeDir = "C:\\Users\\alice",
            dataRootOverride = null,
            localAppData = "C:\\Users\\alice\\AppData\\Local"
        )

        assertEquals(
            Paths.get("C:\\Users\\alice\\AppData\\Local", "PrintBusiness").toAbsolutePath().normalize(),
            resolved
        )
    }

    @Test
    fun `uses macOS application support path`() {
        val resolved = AppDataPaths.resolveDataRoot(
            osName = "Mac OS X",
            homeDir = "/Users/alice",
            dataRootOverride = null,
            localAppData = null
        )

        assertEquals(
            Paths.get("/Users/alice/Library/Application Support/PrintBusiness").toAbsolutePath().normalize(),
            resolved
        )
    }

    @Test
    fun `uses linux local share path`() {
        val resolved = AppDataPaths.resolveDataRoot(
            osName = "Linux",
            homeDir = "/home/alice",
            dataRootOverride = null,
            localAppData = null
        )

        assertEquals(
            Paths.get("/home/alice/.local/share/PrintBusiness").toAbsolutePath().normalize(),
            resolved
        )
    }
}
