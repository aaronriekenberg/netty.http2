package org.aaron.netty.http2

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler
import mu.KLogging

/**
 * Negotiates with the browser if HTTP2 or HTTP is going to be used. Once decided, the Netty
 * pipeline is setup with the correct handlers for the selected protocol.
 */
class Http2OrHttpHandler : ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {

    override fun configurePipeline(ctx: ChannelHandlerContext, protocol: String) {
        when (protocol) {
            ApplicationProtocolNames.HTTP_2 -> ctx.pipeline().addLast(HelloWorldHttp2HandlerBuilder().build())

            ApplicationProtocolNames.HTTP_1_1 -> ctx.pipeline().addLast(HttpServerCodec(),
                    HttpObjectAggregator(MAX_CONTENT_LENGTH),
                    HelloWorldHttp1Handler("ALPN Negotiation"))

            else -> throw IllegalStateException("unknown protocol: $protocol")
        }
    }

    companion object : KLogging() {
        private const val MAX_CONTENT_LENGTH = 1024 * 100
    }
}
