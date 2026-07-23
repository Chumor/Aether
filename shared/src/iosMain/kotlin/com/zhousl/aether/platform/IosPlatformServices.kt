package com.zhousl.aether.platform

import com.zhousl.aether.runtime.NativePickedFileListener
import com.zhousl.aether.runtime.NativeRuntimeHost
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class IosPlatformServices(
    private val host: NativeRuntimeHost,
) : PlatformServices {
    override suspend fun pickFile(imagesOnly: Boolean): PlatformPickedFile? =
        suspendCancellableCoroutine { continuation ->
            host.pickFile(imagesOnly, object : NativePickedFileListener {
                override fun onSelected(name: String, mimeType: String, bytes: ByteArray) {
                    if (continuation.isActive) {
                        continuation.resume(PlatformPickedFile(name, mimeType, bytes))
                    }
                }

                override fun onCancelled() {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onError(message: String) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException(message))
                    }
                }
            })
        }

    override fun openUrl(url: String): Boolean = host.openUrl(url)
    override fun copyText(text: String): Boolean = host.copyText(text)
    override fun shareText(title: String, text: String): Boolean = host.shareText(title, text)
}
