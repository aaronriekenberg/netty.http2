package org.aaron.netty.http2

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.ssl.SslContext
import mu.KLogging

class Http2ServerInitializer(
        private val sslCtx: SslContext) : ChannelInitializer<SocketChannel>() {

    companion object : KLogging()

    override fun initChannel(ch: SocketChannel) {
        logger.info { "initChannel $ch" }
        configureSsl(ch)
    }

    /**
     * Configure the pipeline for TLS NPN negotiation to HTTP/2.
     */
    private fun configureSsl(ch: SocketChannel) {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), Http2OrHttpHandler())
    }

}