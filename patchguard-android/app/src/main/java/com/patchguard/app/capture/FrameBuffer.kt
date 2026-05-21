package com.patchguard.app.capture

import com.patchguard.app.data.ImageMetadata

class FrameBuffer(private val batchSize: Int) {

    data class Frame(val jpeg: ByteArray, val metadata: ImageMetadata)

    private val buffer = mutableListOf<Frame>()

    var onBatchReady: ((List<Frame>) -> Unit)? = null

    @Synchronized
    fun add(frame: Frame) {
        buffer.add(frame)
        if (buffer.size >= batchSize) flush()
    }

    @Synchronized
    fun flush() {
        if (buffer.isEmpty()) return
        val batch = buffer.toList()
        buffer.clear()
        onBatchReady?.invoke(batch)
    }

    @Synchronized
    fun clear() {
        buffer.clear()
    }
}
