package com.omnix.manager.websocket;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.omnix.manager.LogManager;
import com.omnix.manager.recieve.LogBean;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;

@Service
public class WebSocketManager {
	@Autowired
	private SimpMessagingTemplate template;
	
	private Cache<Long, Disposable> realtimeCache;

	@PostConstruct
	public void init() {
		realtimeCache = CacheBuilder.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES).removalListener(new RemovalListener<Long, Disposable>() {
			@Override
			public void onRemoval(RemovalNotification<Long, Disposable> entry) {
				entry.getValue().dispose();
			}
		}).build();
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
				template.convertAndSend("/topic/realtimeLog/" + realtimeLogRegister.getJobId(), lists);
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
		template.convertAndSend("/topic/search/" + searchStatus.getJobId(), searchStatus);
	}

}
