package com.tuna.rtmp.codec;

import com.tuna.rtmp.client.RtmpClientImpl;
import com.tuna.rtmp.domain.RtmpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RtmpEncoder extends MessageToByteEncoder {

	private final RtmpClientImpl rtmpClient;

	public RtmpEncoder(RtmpClientImpl rtmpClient) {
		this.rtmpClient = rtmpClient;
	}

	@Override
	protected void encode(ChannelHandlerContext channelHandlerContext, Object input, ByteBuf out) throws Exception {
		if (input instanceof RtmpMessage) {
			RtmpMessage message = (RtmpMessage) input;
			try {
				if (message.getChunkStreamId() > 64) {
					// TODO
				}
				out.writeByte((message.getFm() << 6) | message.getChunkStreamId());
				if (message.getFm() != 3) {
					out.writeMedium(message.getTimestamp());
				}
				if (message.getFm() < 2) {
					out.writeMedium(message.getPayload().readableBytes());
					out.writeByte(message.getTypeId());
				}
				if (message.getFm() == 0) {
					out.writeIntLE(message.getStreamId());
				}

				// if payload size is greater than chunk size, need split the package.
				while (message.getPayload().readableBytes() > rtmpClient.getChunkSize()) {
					out.writeBytes(message.getPayload(), message.getPayload().readerIndex(), rtmpClient.getChunkSize());
					message.getPayload().skipBytes(rtmpClient.getChunkSize());
					out.writeByte((3 << 6) | message.getChunkStreamId());
				}
				out.writeBytes(message.getPayload(), message.getPayload().readerIndex(), message.getPayload().readableBytes());
				message.getPayload().skipBytes(message.getPayload().readableBytes());
			} finally {
				message.recycle();
			}
		} else {
			throw new RuntimeException("Write a unknow message type");
		}
	}
}
