package com.zhousl.aether.platform

data class PlatformPickedFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
)

interface PlatformServices {
    suspend fun pickFile(imagesOnly: Boolean = false): PlatformPickedFile?
    fun copyText(text: String): Boolean
    fun shareText(title: String, text: String): Boolean
    fun openUrl(url: String): Boolean
}

object NoOpPlatformServices : PlatformServices {
    override suspend fun pickFile(imagesOnly: Boolean): PlatformPickedFile? = null
    override fun copyText(text: String): Boolean = false
    override fun shareText(title: String, text: String): Boolean = false
    override fun openUrl(url: String): Boolean = false
}
