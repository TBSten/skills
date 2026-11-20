package com.example.feedback

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.UIKit.UIDevice
import kotlin.coroutines.resume

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
actual fun httpTransport(endpoint: String, token: String?): FeedbackTransport = FeedbackTransport { json ->
    val url = NSURL.URLWithString(endpoint) ?: return@FeedbackTransport FeedbackResult(false, error = "受け口の URL が未設定か不正: $endpoint")
    val req = NSMutableURLRequest.requestWithURL(url).apply {
        setHTTPMethod("POST")
        setValue("application/json; charset=utf-8", forHTTPHeaderField = "content-type")
        token?.let { setValue(it, forHTTPHeaderField = "x-feedback-token") }
        setHTTPBody(NSString.create(string = json).dataUsingEncoding(NSUTF8StringEncoding))
    }
    suspendCancellableCoroutine { cont ->
        val task = NSURLSession.sharedSession.dataTaskWithRequest(req) { data, response, error ->
            val result = if (error != null) FeedbackResult(false, error = error.localizedDescription)
            else {
                val status = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
                val body = data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() } ?: ""
                parseFeedbackResponse(status, body)
            }
            cont.resume(result)
        }
        cont.invokeOnCancellation { task.cancel() }
        task.resume()
    }
}

actual fun encodePng(image: ImageBitmap): ByteArray? =
    runCatching { Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes }.getOrNull()

actual fun prepareShot(image: ImageBitmap): ImageBitmap = image

actual fun platformEnv(): Map<String, Any?> = UIDevice.currentDevice.let {
    mapOf("os" to "${it.systemName} ${it.systemVersion}", "device" to it.model)
}
