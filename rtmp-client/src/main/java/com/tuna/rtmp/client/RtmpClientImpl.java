package com.tuna.rtmp.client;

import com.tuna.rtmp.codec.RtmpDecoder;
import com.tuna.rtmp.codec.RtmpEncoder;
import com.tuna.rtmp.domain.*;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NetSocketInternal;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.tuna.rtmp.domain.Constants.*;

public class RtmpClientImpl implements RtmpClient {

	private static final Logger logger = LoggerFactory.getLogger(RtmpClient.class);

	private Vertx vertx;

	private RtmpContext context;

	private NetClient netClient;
	private NetSocketInternal socket;
	private AtomicLong transId = new AtomicLong(1);

	private int chunkSize = DEFAULT_CHUNK_SIZE;
	private int ackWindowSize = 500000;

	private Map<Long, Handler<AsyncResult<RtmpClient>>> callBackHandler = new ConcurrentHashMap();
	private Map<Long, String> callBackHandlerType = new ConcurrentHashMap();

	public RtmpClientImpl(Vertx vertx, RtmpContext context) {
		this.vertx = vertx;
		this.context = context;
	}

	@Override
	public void handshake(Handler<AsyncResult<RtmpClient>> handler) {
		NetClientOptions options = new NetClientOptions().setConnectTimeout(6000);
		options.setReconnectAttempts(3).setReconnectInterval(1000);
		options.setLogActivity(context.isLogActivity());
		options.setTcpNoDelay(true);
		options.setTcpKeepAlive(true);
		options.setTcpFastOpen(true);

		netClient = vertx.createNetClient(options);

		Promise<NetSocket> connectPromise = Promise.promise();
		netClient.connect(context.getPort(), context.getHost(), connectPromise);

		connectPromise.future().compose(netSocket -> {
			RtmpClientImpl.this.socket = (NetSocketInternal) netSocket;
			if (context.isLogActivity()) {
				socket.channelHandlerContext().pipeline().addBefore("handler", "logger", new LoggingHandler(LogLevel.INFO));
			}
			socket.channelHandlerContext().pipeline().addBefore("handler", "handshakeDecoder", new FixedLengthFrameDecoder(1 + C1C2_LENGTH * 2));

			ByteBuf byteBuf = socket.channelHandlerContext().alloc().buffer(1 + C1C2_LENGTH);
			ProtocolUtils.handshakeC0C1(byteBuf);
			socket.write(Buffer.buffer(byteBuf));

			Promise<Void> readS0S1S2Promise = Promise.promise();

			socket.messageHandler(msg -> {
				if (msg instanceof ByteBuf) {
					// write S1 to the server
					((ByteBuf) msg).skipBytes(1);
					((ByteBuf) msg).writerIndex(((ByteBuf) msg).readerIndex() + C1C2_LENGTH);
					socket.write(Buffer.buffer((ByteBuf) msg), v -> {
						readS0S1S2Promise.handle(v);
					});
				}
			});
			return readS0S1S2Promise.future();
		}).setHandler(v -> {
			try {
				if (v.succeeded()) {
					socket.channelHandlerContext().pipeline().remove("handshakeDecoder");
					socket.channelHandlerContext().pipeline().addBefore("handler", "rtmpEncoder", new RtmpEncoder(RtmpClientImpl.this));
					socket.channelHandlerContext().pipeline().addBefore("handler", "rtmpDecoder", new RtmpDecoder(RtmpClientImpl.this));
					socket.messageHandler(this::rtmpMessageHandler);
					handler.handle(Future.succeededFuture(RtmpClientImpl.this));
				} else {
					handler.handle(Future.failedFuture(v.cause()));
				}
			} catch (Exception e) {
				handler.handle(Future.failedFuture(e));
			}
		});
	}

	protected void rtmpMessageHandler(Object msg) {
		if (msg instanceof RtmpMessage) {
			RtmpMessage rtmpMessage = (RtmpMessage) msg;
			switch (rtmpMessage.getTypeId()) {
			case MSG_TYPE_CHUNK_SIZE:
				int newChunkSize = rtmpMessage.getPayload().readInt();
				logger.info("chunk size from {} change to {} ", this.chunkSize, newChunkSize);
				this.chunkSize = newChunkSize;
				socket.writeMessage(MessageBuilder.createSetChunkSize(chunkSize));
				break;
			case MSG_TYPE_ABORT:
				break;
			case MSG_TYPE_ACK:
				break;
			case MSG_TYPE_USER:
				break;
			case MSG_TYPE_ACK_SIZE:
				socket.writeMessage(MessageBuilder.createAcknowledgementWindowSize(ackWindowSize));
				break;
			case MSG_TYPE_BANDWIDTH:
				socket.writeMessage(MessageBuilder.createSetPeerBandWidth(ackWindowSize));
				break;
			case MSG_TYPE_EDGE:
				break;
			case MSG_TYPE_AUDIO:
				break;
			case MSG_TYPE_VIDEO:
				break;
			case MSG_TYPE_AMF3_META:
				break;
			case MSG_TYPE_AMF3_SHARED:
				break;
			case MSG_TYPE_AMF3_CMD:
				break;
			case MSG_TYPE_AMF_META:
				break;
			case MSG_TYPE_AMF_SHARED:
				break;
			case MSG_TYPE_AMF_CMD:
				handAmfCommandResponse(rtmpMessage);
				break;
			case MSG_TYPE_UNDEFINED:
				break;
			}
			rtmpMessage.recycle();
		}
	}

	protected void handAmfCommandResponse(RtmpMessage rtmpMessage) {
		String tmp = ProtocolUtils.readAmfString(rtmpMessage.getPayload());
		long tsId = ProtocolUtils.readAmfNumber(rtmpMessage.getPayload());
		if (callBackHandler.containsKey(tsId)) {
			callBackHandler.get(tsId).handle(Future.succeededFuture(RtmpClientImpl.this));
			callBackHandler.remove(tsId);
		}

		// Process rtmp server allocate information.
		if (callBackHandlerType.containsKey(tsId)) {
			if (callBackHandlerType.get(tsId).equals(AMF_CMD_CREATE_STREAM)) {
				ProtocolUtils.readAmfNull(rtmpMessage.getPayload());
				long streamId = ProtocolUtils.readAmfNumber(rtmpMessage.getPayload());
				context.setStreamId((int) streamId);
			}
			callBackHandlerType.remove(tsId);
		}
	}

	@Override
	public void connect(Handler<AsyncResult<RtmpClient>> handler) {
		long id = transId.getAndIncrement();
		callBackHandler.put(id, handler);
		socket.writeMessage(MessageBuilder.createConnect(id, context));
		vertx.setTimer(context.getRtimeout(), timerId -> {
			if (callBackHandler.containsKey(id)) {
				callBackHandler.get(id).handle(Future.failedFuture("Request timeout"));
				callBackHandler.remove(id);
			}
		});
	}

	public void acknowledgement(long size, Handler<AsyncResult<Void>> handler) {
		socket.writeMessage(MessageBuilder.createAcknowledgement((int) size), handler);
	}

	public void acknowledgementWindowSize(long size, Handler<AsyncResult<Void>> handler) {
		socket.writeMessage(MessageBuilder.createAcknowledgementWindowSize((int) size), handler);
	}

	@Override
	public void createStream(Handler<AsyncResult<RtmpClient>> handler) {
		long id = transId.getAndIncrement();
		callBackHandler.put(id, handler);
		callBackHandlerType.put(id, AMF_CMD_CREATE_STREAM);
		socket.writeMessage(MessageBuilder.createStream(id));
		vertx.setTimer(context.getRtimeout(), timerId -> {
			if (callBackHandler.containsKey(id)) {
				callBackHandler.get(id).handle(Future.failedFuture("Request timeout"));
				callBackHandler.remove(id);
				callBackHandlerType.remove(id);
			}
		});
	}

	@Override
	public void fcPublish(Handler<AsyncResult<RtmpClient>> handler) {
		long id = transId.getAndIncrement();
		socket.writeMessage(MessageBuilder.createFCPublish(id, context), v -> {
			if (v.succeeded()) {
				handler.handle(Future.succeededFuture(RtmpClientImpl.this));
			} else {
				handler.handle(Future.failedFuture(v.cause()));
			}
		});
	}

	@Override
	public void publish(Handler<AsyncResult<RtmpClient>> handler) {
		long id = transId.getAndIncrement();
		socket.writeMessage(MessageBuilder.createPublish(id, context), v -> {
			if (v.succeeded()) {
				handler.handle(Future.succeededFuture(RtmpClientImpl.this));
			} else {
				handler.handle(Future.failedFuture(v.cause()));
			}
		});
	}

	@Override
	public void doAllPublishSteps(Handler<AsyncResult<RtmpClient>> handler) {
		Promise<RtmpClient> handShakePromise = Promise.promise();
		handshake(handShakePromise);
		handShakePromise.future().compose(client -> {
			Promise<RtmpClient> connectPromise = Promise.promise();
			client.connect(connectPromise);
			return connectPromise.future();
		}).compose(client -> {
			Promise<RtmpClient> createStream = Promise.promise();
			client.createStream(createStream);
			return createStream.future();
		}).compose(client -> {
			Promise<RtmpClient> fcPublish = Promise.promise();
			client.fcPublish(fcPublish);
			return fcPublish.future();
		}).compose(client -> {
			Promise<RtmpClient> publish = Promise.promise();
			client.publish(publish);
			return publish.future();
		}).setHandler(handler);
	}

	@Override
	public void sendFlvVideo(int pts, ByteBuf payload, Handler<AsyncResult<Void>> handler) {
		socket.writeMessage(MessageBuilder.createVideo(pts, payload, context), handler);
	}

	@Override
	public void sendFlvAudeo(int pts, ByteBuf payload, Handler<AsyncResult<Void>> handler) {
		socket.writeMessage(MessageBuilder.createAudeo(pts, payload, context), handler);
	}

	@Override
	public void sendFlvVideo(int pts, ByteBuf payload) {
		socket.writeMessage(MessageBuilder.createVideo(pts, payload, context));
	}

	@Override
	public void sendFlvAudeo(int pts, ByteBuf payload) {
		socket.writeMessage(MessageBuilder.createAudeo(pts, payload, context));
	}

	@Override
	public void sendH264Video(int pts, int dts, ByteBuf payload, RtmpContext context) {
		int nalType = payload.getByte(payload.readerIndex()) & 0x1F;
		if (H264Utils.isPPS(nalType)) {
			context.getH264Pps().clear();
			context.getH264Pps().writeBytes(payload);
			context.setH264PpsChanged(true);
			return;
		}
		if (H264Utils.isSPS(nalType)) {
			context.getH264Sps().clear();
			context.getH264Sps().writeBytes(payload);
			context.setH264SpsChanged(true);
			return;
		}

		if (H264Utils.isIgnore(nalType)) {
			logger.info("the video data is ignored, nalType:{}", nalType);
			return;
		}

		if (context.isH264PpsChanged() || context.isH264SpsChanged()) {
			if (context.getH264Pps().readableBytes() < 1 || context.getH264Sps().readableBytes() < 1) {
				// SPS or PPS missing
				return;
			}
			RtmpMessage message = MessageBuilder.createVideo(pts, context);
			/**
			 * FrameType and codecId
			 *
			 * <p>
			 * FrameType UB[4] 1:keyframe 2:inter frame 3:disposable inter frame 4:generated keyframe 5: video info/command frame
			 * </p>
			 * <p>
			 * CodecId UB[4] 1: JPEG 2: Sorenson H.263 3: Screen Video 4: No2 VP6 5: On2 VP6 with alpha channel 6: Screen video version 2 7: AVC
			 * </p>
			 */
			message.getPayload().writeByte(0x17);
			/**
			 * AVCPacketType
			 *
			 * 0: AVC sequence header 1: AVC NALU 2:AVC end of sequence.
			 */
			message.getPayload().writeByte(0);
			// Composition time offset
			message.getPayload().writeMedium(0);

			// configurationVersion
			message.getPayload().writeByte(1);
			// AVCProfileIndication
			message.getPayload().writeByte(context.getH264Sps().getByte(context.getH264Sps().readerIndex() + 1));
			// profile_compatibility
			message.getPayload().writeByte(0);
			// AVCLevelIndication
			message.getPayload().writeByte(context.getH264Sps().getByte(context.getH264Sps().readerIndex() + 3));
			// lengthSizeMinusOne, or NAL_unit_length, always use 4bytes size,
			// so we always set it to 0x03.
			message.getPayload().writeByte(3);

			// SPS
			// numOfSequenceParameterSets, always 1
			message.getPayload().writeByte(1);
			// sequenceParameterSetLength
			message.getPayload().writeShort(context.getH264Sps().readableBytes());
			// sequenceParameterSetNALUnit
			message.getPayload().writeBytes(context.getH264Sps());

			// PPS
			// numOfSequenceParameterSets, always 1
			message.getPayload().writeByte(1);
			// sequenceParameterSetLength
			message.getPayload().writeShort(context.getH264Pps().readableBytes());
			// sequenceParameterSetNALUnit
			message.getPayload().writeBytes(context.getH264Pps());
			socket.writeMessage(message);

			context.setH264PpsChanged(false);
			context.setH264SpsChanged(false);
			context.setH264SpsPpsSent(true);
		}

		if (context.isH264SpsPpsSent()) {
			RtmpMessage message = MessageBuilder.createVideo(pts, context);
			/**
			 * FrameType and codecId
			 *
			 * <p>
			 * FrameType UB[4] 1:keyframe 2:inter frame 3:disposable inter frame 4:generated keyframe 5: video info/command frame
			 * </p>
			 * <p>
			 * CodecId UB[4] 1: JPEG 2: Sorenson H.263 3: Screen Video 4: No2 VP6 5: On2 VP6 with alpha channel 6: Screen video version 2 7: AVC
			 * </p>
			 */
			if (H264Utils.isKeyFrame(nalType)) {
				message.getPayload().writeByte(0x17);
			} else {
				message.getPayload().writeByte(0x27);
			}
			/**
			 * AVCPacketType
			 *
			 * 0: AVC sequence header 1: AVC NALU 2:AVC end of sequence.
			 */
			message.getPayload().writeByte(1);
			// Composition time offset
			message.getPayload().writeMedium(pts - dts);

			// NALUnitLength: 4bytes size of nalu:
			message.getPayload().writeInt(payload.readableBytes());
			// NALUnit Nbytes of nalu.
			message.getPayload().writeBytes(payload);
			socket.writeMessage(message);
		}
	}

	@Override
	public void close(Handler<AsyncResult<Void>> handler) {
		socket.close(v -> {
			netClient.close();
			handler.handle(v);
		});
	}

	@Override
	public void close() {
		socket.close();
		netClient.close();
	}

	@Override
	public void setCloseHandler(Handler<Void> handler) {
		socket.closeHandler(handler);
	}

	public int getChunkSize() {
		return chunkSize;
	}
}
