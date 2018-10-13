package org.aaron.netty.http2

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled.copiedBuffer
import io.netty.buffer.Unpooled.unreleasableBuffer
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http2.*
import io.netty.util.CharsetUtil
import mu.KLogging

/**
 * A simple handler that responds with the message "Hello World!".
 */
class HelloWorldHttp2Handler(decoder: Http2ConnectionDecoder,
                             encoder: Http2ConnectionEncoder,
                             initialSettings: Http2Settings) :
        Http2ConnectionHandler(decoder, encoder, initialSettings), Http2FrameListener {

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn(cause) { "exceptionCaught" }
        ctx.close()
    }

    /**
     * Sends a "Hello World" DATA frame to the client.
     */
    private fun sendResponse(ctx: ChannelHandlerContext, streamId: Int, payload: ByteBuf) {
        // Send a frame for the response status
        val headers = DefaultHttp2Headers().status(OK.codeAsText())
        encoder().writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise())
        encoder().writeData(ctx, streamId, payload, 0, true, ctx.newPromise())

        // no need to call flush as channelReadComplete(...) will take care of it.
    }

    override fun onDataRead(ctx: ChannelHandlerContext, streamId: Int, data: ByteBuf, padding: Int, endOfStream: Boolean): Int {
        val processed = data.readableBytes() + padding
        if (endOfStream) {
            sendResponse(ctx, streamId, data.retain())
        }
        return processed
    }

    override fun onHeadersRead(ctx: ChannelHandlerContext, streamId: Int,
                               headers: Http2Headers, padding: Int, endOfStream: Boolean) {
        if (endOfStream) {
            val content = ctx.alloc().buffer()
            content.writeBytes(RESPONSE_BYTES.duplicate())
            ByteBufUtil.writeAscii(content, " - via HTTP/2\n")
            ByteBufUtil.writeAscii(content, "streamId=$streamId method=${headers[":method"]} path=${headers[":path"]}")
            sendResponse(ctx, streamId, content)
        }
    }

    override fun onHeadersRead(ctx: ChannelHandlerContext, streamId: Int, headers: Http2Headers, streamDependency: Int,
                               weight: Short, exclusive: Boolean, padding: Int, endOfStream: Boolean) {
        onHeadersRead(ctx, streamId, headers, padding, endOfStream)
    }

    override fun onPriorityRead(ctx: ChannelHandlerContext, streamId: Int, streamDependency: Int,
                                weight: Short, exclusive: Boolean) {
    }

    override fun onRstStreamRead(ctx: ChannelHandlerContext, streamId: Int, errorCode: Long) {}

    override fun onSettingsAckRead(ctx: ChannelHandlerContext) {}

    override fun onSettingsRead(ctx: ChannelHandlerContext, settings: Http2Settings) {}

    override fun onPingRead(ctx: ChannelHandlerContext, data: Long) {}

    override fun onPingAckRead(ctx: ChannelHandlerContext, data: Long) {}

    override fun onPushPromiseRead(ctx: ChannelHandlerContext, streamId: Int, promisedStreamId: Int,
                                   headers: Http2Headers, padding: Int) {
    }

    override fun onGoAwayRead(ctx: ChannelHandlerContext, lastStreamId: Int, errorCode: Long, debugData: ByteBuf) {}

    override fun onWindowUpdateRead(ctx: ChannelHandlerContext, streamId: Int, windowSizeIncrement: Int) {}

    override fun onUnknownFrame(ctx: ChannelHandlerContext, frameType: Byte, streamId: Int,
                                flags: Http2Flags, payload: ByteBuf) {
    }

    companion object : KLogging() {
        internal val RESPONSE_BYTES = unreleasableBuffer(copiedBuffer("Hello World", CharsetUtil.UTF_8))
    }
}
