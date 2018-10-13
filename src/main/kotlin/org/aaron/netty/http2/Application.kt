package org.aaron.netty.http2

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http2.Http2SecurityUtil
import io.netty.handler.ssl.*
import io.netty.handler.ssl.util.SelfSignedCertificate
import mu.KLogging

class Http2Server(
        private val port: Int) {

    companion object : KLogging()

    fun run() {
        // Configure SSL.
        val sslProvider = if (OpenSsl.isAlpnSupported()) SslProvider.OPENSSL else SslProvider.JDK
        logger.info("sslProvider = ${sslProvider}")

        val ssc = SelfSignedCertificate()
        val sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(sslProvider)
                /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                 * Please refer to the HTTP/2 specification for cipher requirements. */
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build()

        // Configure the server.
        val group = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.option(ChannelOption.SO_BACKLOG, 1024)
            b.group(group)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(Http2ServerInitializer(sslCtx))

            val ch = b.bind(port).sync().channel()

            logger.info("Open your HTTP/2-enabled web browser and navigate to https://127.0.0.1:${port}/")

            ch.closeFuture().sync()

        } finally {
            group.shutdownGracefully()
        }
    }
}

fun main(args: Array<String>) {
    Http2Server(port = 8000).run()
}