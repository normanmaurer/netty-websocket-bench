package io.wasted.netty.websocket.bench

import io.netty.bootstrap._
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.websocketx.{ WebSocketClientHandshakerFactory, WebSocketVersion }
import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpClientCodec, HttpObjectAggregator }

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._

object Client extends App with Logger { PS =>
  override def main(args: Array[String]) {
    start(new URI(args(0)), args(1).toInt)
  }

  private var eventLoop: Option[NioEventLoopGroup] = None
  private var spawned = 0
  val connected = new AtomicInteger(0)
  val messages = new AtomicInteger(0)

  def start(uri: URI, clientCount: Int) {
    // JVM Checking
    val jvmVersion = System.getProperty("java.runtime.version")
    if (!jvmVersion.matches("^1.[78].*$")) {
      error("Java Runtime %s is not supported", jvmVersion)
      return
    }

    eventLoop = Some(new NioEventLoopGroup)
    val boot = new Bootstrap().channel(classOf[NioSocketChannel])
    while (spawned < clientCount) {
      val handsh = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders())
      val handler = new ClientHandler(handsh)
      val b = boot.clone()
      b.group(eventLoop.get).handler(new ChannelInitializer[SocketChannel] {
        def initChannel(ch: SocketChannel) {
          val p = ch.pipeline()
          p.addLast("http-codec", new HttpClientCodec())
          p.addLast("aggregator", new HttpObjectAggregator(8192))
          p.addLast("ws-handler", handler)
        }
      })
      val ch = b.connect(uri.getHost, uri.getPort).sync().channel()
      //handler.handshakeFuture() //.sync()
      spawned += 1
    }

    info("All %s launched".format(clientCount))

    Schedule.again(() => {
      info("-- Stats -- connected: %s -- messages: %s".format(connected.get(), messages.get()))
    }, 1.seconds, 1.seconds)

    // Add Shutdown Hook to cleanly shutdown Netty
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() { PS.stop() }
    })
  }

  def stop() {
    info("Shutting down")

    // Shut down all event loops to terminate all threads.
    eventLoop.map(_.shutdownGracefully())
    eventLoop = None

    info("Shutdown complete")

    info("-- Stats --")
    info("connected: %s".format(connected.get()))
    info("messages: %s".format(messages.get()))
    info("--")
  }
}

