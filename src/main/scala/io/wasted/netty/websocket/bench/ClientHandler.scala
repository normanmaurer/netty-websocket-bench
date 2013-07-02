package io.wasted.netty.websocket.bench

import io.netty.channel._
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx._

/**
 * Handles incoming requests which will be sent to an AuthActor
 */
@ChannelHandler.Sharable
class ClientHandler(handshaker: WebSocketClientHandshaker) extends SimpleChannelInboundHandler[Object] {

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace()
    if (ctx.channel.isOpen) ctx.close
  }

  override def handlerAdded(ctx: ChannelHandlerContext) {
    //Client.connected.incrementAndGet()
  }

  override def channelActive(ctx: ChannelHandlerContext) {
    handshaker.handshake(ctx.channel())
  }

  override def channelUnregistered(ctx: ChannelHandlerContext) {
    //Client.connected.decrementAndGet()
  }

  def messageReceived(ctx: ChannelHandlerContext, msg: Object) {
    msg match {
      case _ if !handshaker.isHandshakeComplete =>
        handshaker.finishHandshake(ctx.channel(), msg.asInstanceOf[FullHttpResponse])

      case wsf: TextWebSocketFrame =>
      // do something

      case wsf: CloseWebSocketFrame =>
        //ctx.channel().write(new CloseWebSocketFrame())
        ctx.close()

      case f: Object =>
        ctx.close()
    }
  }
}

