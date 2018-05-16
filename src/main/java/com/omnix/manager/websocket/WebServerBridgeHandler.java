package com.omnix.manager.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class WebServerBridgeHandler extends ChannelInboundHandlerAdapter {
	private ChannelHandlerContext ctx;

	public void sendMessage(String message) {
		if (ctx != null) {
			ByteBuf buf = new ByteBufferSupport(message).getBuffer();
			ctx.writeAndFlush(buf);
			buf.release();
		} else {
			// nothing
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}
}
