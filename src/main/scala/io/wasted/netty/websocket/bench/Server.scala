package io.wasted.netty.websocket.bench

import io.netty.bootstrap._
import io.netty.buffer._
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.{ HttpResponseEncoder, HttpObjectAggregator, HttpRequestDecoder }
import io.netty.handler.timeout._

import scala.util.{ Try, Success, Failure }
import scala.concurrent.duration._

object Server extends App with Logger { PS =>
  override def main(args: Array[String]) { start() }
  private var listeners: List[java.net.InetSocketAddress] = List()

  private var eventLoop1: Option[NioEventLoopGroup] = None
  private var eventLoop2: Option[NioEventLoopGroup] = None

  def listeningOn() = listeners

  def start() {
    // OS Checking
    val os = System.getProperty("os.name").toLowerCase
    if (!(os.contains("nix") || os.contains("nux") || os.contains("bsd") || os.contains("mac") || os.contains("sunos")) || os.contains("win")) {
      error("%s is not a supported platform", System.getProperty("os.name"))
      return
    }

    // JVM Checking
    val jvmVersion = System.getProperty("java.runtime.version")
    if (!jvmVersion.matches("^1.[78].*$")) {
      error("Java Runtime %s is not supported", jvmVersion)
      return
    }

    val listeners: List[java.net.InetSocketAddress] = List("0.0.0.0:5555").flatMap {
      case ipv4: String if ipv4.matches("""\d+\.\d+\.\d+\.\d+:\d+""") =>
        val split = ipv4.split(":")
        Try(new java.net.InetSocketAddress(split(0), split(1).toInt)).toOption
      case ipv6: String if ipv6.matches("""\[[0-9a-fA-F:]+\]:\d+""") =>
        val split = ipv6.split("]:")
        val addr = split(0).replaceFirst("\\[", "")
        Try(new java.net.InetSocketAddress(java.net.InetAddress.getByName(addr), split(1).toInt)).toOption
      case _ => None
    }

    eventLoop1 = Some(new NioEventLoopGroup)
    eventLoop2 = Some(new NioEventLoopGroup)
    val maxChunkSize = 8192
    val maxHeaderSize = 8192
    val maxInitLength = 1024
    val maxContentLength = 1024 * 1024

    Try {
      listeners.map { addr =>
        val srv = new ServerBootstrap
        val chan = srv.group(eventLoop1.get, eventLoop2.get)
          .localAddress(addr)
          .channel(classOf[NioServerSocketChannel])
          .childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
          .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
          .childOption[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
          .childOption[java.lang.Integer](ChannelOption.SO_LINGER, 0)
          .childHandler(new ChannelInitializer[SocketChannel] {
            override def initChannel(ch: SocketChannel) {
              val p = ch.pipeline()
              p.addLast("decoder", new HttpRequestDecoder(maxInitLength, maxHeaderSize, maxChunkSize))
              p.addLast("aggregator", new HttpObjectAggregator(maxContentLength))
              p.addLast("encoder", new HttpResponseEncoder())
              p.addLast("requestHandler", ServerHandler)
            }
          })
        //chan.childOption[ByteBufAllocator](ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
        //chan.childOption[java.lang.Integer](ChannelOption.SO_SNDBUF, i)
        //chan.childOption[java.lang.Integer](ChannelOption.SO_RCVBUF, i)
        srv.bind().syncUninterruptibly()
        info("Listening on %s:%s", addr.getAddress.getHostAddress, addr.getPort)
        srv
      }
    } match {
      case Success(v) => Some(v)
      case Failure(f) => error("Unable to bind to that ip:port combination. Check your configuration."); stop(); None
    }

    info("Ready")

    Schedule.again(() => {
      info("-- Stats -- connected: %s".format(ServerHandler.connected.get()))
    }, 1.seconds, 1.seconds)

    // Add Shutdown Hook to cleanly shutdown Netty
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() { PS.stop() }
    })
  }

  def stop() {
    info("Shutting down")

    // Shut down all event loops to terminate all threads.
    eventLoop1.map(_.shutdownGracefully())
    eventLoop1 = None

    eventLoop2.map(_.shutdownGracefully())
    eventLoop2 = None

    listeners = List()
    info("Shutdown complete")
  }
}

