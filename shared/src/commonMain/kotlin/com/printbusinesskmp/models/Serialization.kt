package com.printbusinesskmp.models

import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val modelsSerializersModule = SerializersModule {
    contextual(Instant::class) { kotlinx.datetime.serializers.UtcOffsetIso8601Serializer }
}

val modelsJson = Json {
    serializersModule = modelsSerializersModule
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
}