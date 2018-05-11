package com.omnix.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.omnix.manager.IndexManager;
import com.omnix.manager.search.SearchResult;
import com.omnix.manager.search.SearchType;

public class SearchUtils {

	private static final DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String HISTOGRAM_1_HOUR_TICK = "yyyyMMdd000000";
	private static final String TIME_FORMAT = "yyyyMMddHHmmss";

	public static List<LocalDateTime> getBetweenDate(SearchType searchType, String dateRange) {
		LocalDateTime now = LocalDateTime.now();

		/** 검색 범위 계산 */
		LocalDateTime start = null;
		LocalDateTime finish = null;

		if (searchType.equals(SearchType.CUSTOM)) {
			String[] temp = StringUtils.split(dateRange, "~");

			String temp1 = StringUtils.trim(temp[0]);
			String temp2 = StringUtils.trim(temp[1]);

			if (temp[0].length() == 10) {
				temp1 += " 00:00:00";
				temp2 += " 00:00:00";
			} else if (temp[0].length() == 13) {
				temp1 += "00:00";
				temp2 += "00:00";
			}

			start = parseDate(temp1, defaultFormatter);
			finish = parseDate(temp2, defaultFormatter).minusNanos(1000L);

		} else {
			start = searchType.getDay(now);
			finish = now;
		}

		/** 미래인경우 강제로 현재 시간으로 조정한다. */
		if (finish.isAfter(now)) {
			finish = now;
		}

		return Arrays.asList(start, finish);
	}

	public static LocalDateTime parseDate(String date, DateTimeFormatter formatter) {
		return LocalDateTime.parse(date, formatter);
	}

	/**
	 * 대상일 계산
	 */
	public static List<String> searchTargetList(LocalDateTime start, LocalDateTime finish, boolean reverse) {
		if (finish.isBefore(start)) {
			throw new RuntimeException("check end date start : " + start + ", finish : " + finish);
		}

		List<String> targets = new ArrayList<>();

		long start2 = NumberUtils.toLong(start.format(IndexManager.HOUR_FORMAT));
		long finish2 = NumberUtils.toLong(finish.format(IndexManager.HOUR_FORMAT));

		LocalDateTime runTime = finish.withNano(0).withSecond(0).withMinute(0);

		for (int i = 0; i < 4320; i++) { // 최대 180일
			LocalDateTime now = runTime.minusHours(i);

			long now2 = NumberUtils.toLong(now.format(IndexManager.HOUR_FORMAT));

			if (now2 >= start2 && now2 <= finish2) {
				targets.add(now.format(IndexManager.HOUR_FORMAT));
			}

			if (now2 < start2) {
				break;
			}
		}

		if (!reverse) {
			Collections.reverse(targets);
		}

		return targets;
	}

	/**
	 * histogram의 시간 단위 계산
	 * 
	 * @param startDate
	 * @param endDate
	 * @return <date, 0>
	 */
	public static void make1HourHistogram(SearchResult searchResult, int min) {
		String[] date = searchResult.getDate().split("~");

		Date startDate = DateUtils.getDate(date[0].trim());
		Date endDate = DateUtils.getDate(date[1].trim());

		/** 초 단위 까지 표시 */
		Map<String, Integer> histogram = searchResult.getHistogram().entrySet().stream().collect(Collectors.toMap(e -> e.getKey() + "0000", e -> e.getValue()));
		Date loopStart = DateUtils.getDateTime(DateUtils.format(HISTOGRAM_1_HOUR_TICK, startDate), TIME_FORMAT, null);

		while (true) {
			if (loopStart.after(endDate)) {
				break;
			}

			if (DateUtils.addMinutes(loopStart, min).before(startDate) || DateUtils.addMinutes(loopStart, min).equals(startDate)) {
				loopStart = DateUtils.addMinutes(loopStart, min);
				continue;
			}
			String key = DateUtils.format(TIME_FORMAT, loopStart);
			/** 값이 없을 경우, 빈 값을 채워준다. */
			histogram.putIfAbsent(key, 0);

			loopStart = DateUtils.addMinutes(loopStart, min);
		}
		Map<String, Integer> sortedMap = histogram.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
		searchResult.setHistogram(sortedMap);
	}
}
