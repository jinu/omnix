package com.omnix.manager.websocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.omnix.config.Config;
import com.omnix.manager.LogManager;
import com.omnix.manager.recieve.LogBean;
import com.omnix.util.JsonUtils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;

@Service
public class WebSocketManager {
	@Autowired
	private SimpMessagingTemplate template;
	@Autowired
	private Config config;

	private final WebServerBridgeHandler webServerBridgeHandler = new WebServerBridgeHandler();
	private Cache<Long, Disposable> realtimeCache;
	
	private final NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
	private ChannelFuture channelFuture;
	
	private boolean serverInitFlag = false;
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	

	@PostConstruct
	public void init() {
		realtimeCache = CacheBuilder.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES).removalListener(new RemovalListener<Long, Disposable>() {
			@Override
			public void onRemoval(RemovalNotification<Long, Disposable> entry) {
				entry.getValue().dispose();
			}
		}).build();

		try {
			int port = config.getWebBridgePort();
			initWebServerBridge(port);
		} catch (Exception e) {
			logger.error("webserver bridge init error", e);
		}
	}

	/**
	 * 리얼타임 리스너 등록
	 */
	public void realtimeLogRegister(RealtimeLogRegister realtimeLogRegister) {
		Predicate<LogBean> filterPredicate = null;
		if (StringUtils.isEmpty(realtimeLogRegister.getSearch())) {
			filterPredicate = (logBean) -> {
				return true;
			};
		} else {
			filterPredicate = (logBean) -> {
				return StringUtils.contains(logBean.getText(), realtimeLogRegister.getSearch());
			};
		}

		Disposable disposable = LogManager.PUBSUB.filter(filterPredicate).buffer(1000L, TimeUnit.MILLISECONDS).subscribe(texts -> {
			List<RealtimeLog> lists = texts.stream().limit(50L).map(logBean -> {
				return new RealtimeLog(logBean);
			}).collect(Collectors.toList());

			if (lists.size() > 0) {
				String key = "/topic/realtimeLog/" + realtimeLogRegister.getJobId();
				template.convertAndSend(key, lists);
				sendWebServerBridgeMessage(key, lists);
			}

		});

		realtimeCache.invalidate(realtimeLogRegister.getJobId());
		realtimeCache.put(realtimeLogRegister.getJobId(), disposable);
	}

	public void realtimeLogUnRegister(RealtimeLogRegister realtimeLogRegister) {
		realtimeCache.invalidate(realtimeLogRegister.getJobId());
	}

	/**
	 * 현재 검색 상태를 전송함
	 */
	public void sendSearchStatus(SearchStatus searchStatus) {
		String key = "/topic/search/" + searchStatus.getJobId();
		template.convertAndSend(key, searchStatus);
		sendWebServerBridgeMessage(key, searchStatus);
	}

	/**
	 * TCP 서버 할당
	 * @throws InterruptedException 
	 */
	public void initWebServerBridge(int port) throws InterruptedException {
		if (port == 0) {
			return;
		}
		
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		serverBootstrap.group(workerGroup);

		serverBootstrap.channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				int maxBufferSize = 1024 * 1024 * 10; // 10M
				ch.pipeline().addLast(new DelimiterBasedFrameDecoder(maxBufferSize, Delimiters.lineDelimiter()), webServerBridgeHandler);
			}
		});
		
		channelFuture = serverBootstrap.bind(port).sync();
		serverInitFlag = true;
	}

	public void sendWebServerBridgeMessage(String key, Object payload) {
		if (!serverInitFlag) {
			return;
		}
		
		Map<String, Object> map = new HashMap<>();
		map.put("key", key);
		map.put("payload", payload);
		
		String message = JsonUtils.fromObjInline(map);
		webServerBridgeHandler.sendMessage(message);
	}

	protected void applyConnectionOptions(ServerBootstrap bootstrap) {
		bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
		bootstrap.childOption(ChannelOption.SO_KEEPALIVE, false);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
	}
	
	public void destroy() {
		workerGroup.shutdownGracefully();
		try {
			ChannelFuture f = channelFuture.channel().closeFuture().sync();
			if (!f.isSuccess()) {
				logger.error("close channel error");
			}
		} catch (Exception e) {
			logger.error("close channel error", e);
		}
		
		logger.info("websocket manager close complete");
	}

}
