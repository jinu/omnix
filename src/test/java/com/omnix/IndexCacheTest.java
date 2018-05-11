package com.omnix;

import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.junit.Test;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class IndexCacheTest {
	@Test
	public void test1() {
		LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder().maximumSize(4L).removalListener(new RemovalListener<Integer, Integer>() {
			@Override
			public void onRemoval(RemovalNotification<Integer, Integer> entry) {
				System.out.println("remove index key" + entry.getKey());
			}
		}).build(CacheLoader.<Integer, Integer>from(indexKey -> {
			System.out.println("create index key" + indexKey);
			return indexKey;
		}));
		
		IntStream.range(0, 10).boxed().parallel().forEach(i -> {
			try {
				cache.get(i);
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		});
		
	}
}
