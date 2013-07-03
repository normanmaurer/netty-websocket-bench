package io.wasted.netty.websocket.bench

import io.netty.channel._
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx._
import java.util.concurrent.atomic.AtomicInteger
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor

/**
 * Handles incoming requests which will be sent to an AuthActor
 */
@ChannelHandler.Sharable
object ServerHandler extends SimpleChannelInboundHandler[Object] with Logger {

  val connected = new AtomicInteger(0)
  val clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    debug(cause.toString, cause)
    if (ctx.channel.isOpen) ctx.close
  }

  override def channelActive(ctx: ChannelHandlerContext) {
    connected.incrementAndGet()
    clients.add(ctx.channel())
  }

  override def channelUnregistered(ctx: ChannelHandlerContext) {
    connected.decrementAndGet()
  }

  def messageReceived(ctx: ChannelHandlerContext, msg: Object) {
    msg match {
      case http: FullHttpRequest =>
        handleHttp(ctx, http)

      case wsf: TextWebSocketFrame =>
      // do something

      case wsf: WebSocketFrame =>
        ctx.channel().write(new CloseWebSocketFrame())
        ctx.close()

      case f: Object =>
        info("Unhandled Request: " + f.toString)
        ctx.close()
    }
  }

  private def handleHttp(ctx: ChannelHandlerContext, req: FullHttpRequest) {
    if (!req.getDecoderResult.isSuccess) {
      val f = ctx.channel().write(new HttpResponseStatus(400, "Bad Request (unable to decode)"))
      f.addListener(ChannelFutureListener.CLOSE)
      return
    }

    val headers = OurHttpHeaders.get(req)
    // WebSocket Handshake needed?
    if (headers.get("Upgrade").getOrElse("").toLowerCase != "websocket") {
      debug("Not a WebSocket Request")
      val f = ctx.channel().write(new HttpResponseStatus(400, "Bad Request (no websocket upgrade header)"))
      f.addListener(ChannelFutureListener.CLOSE)
      return
    }

    debug("Upgrading to WebSockets upon client request! " + ctx.channel().id())
    val proto = if (ctx.channel().pipeline.get("ssl") != null) "wss" else "ws"
    // Kill the shitty deflater at this stage
    if (ctx.pipeline().get("deflater") != null) ctx.pipeline().remove("deflater")
    // Handshake
    val location = proto + "://" + req.headers.get(HttpHeaders.Names.HOST) + "/"
    val factory = new WebSocketServerHandshakerFactory(location, null, false)
    val handshaker: WebSocketServerHandshaker = factory.newHandshaker(req)
    if (handshaker != null) handshaker.handshake(ctx.channel(), req)
    else WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel())
  }
}

