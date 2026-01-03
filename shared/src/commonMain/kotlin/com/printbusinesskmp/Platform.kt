package com.printbusinesskmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform