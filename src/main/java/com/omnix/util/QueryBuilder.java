package com.omnix.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StrMatcher;
import org.apache.commons.text.StrTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.InetAddressPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.springframework.util.CollectionUtils;

import com.google.common.net.InetAddresses;
import com.omnix.manager.TsmAnalyzer;
import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.LogFieldType;

public class QueryBuilder {
	/** text */
	private String text;
	/** pointer */
	private int pointer;
	/** depth */
	private int depth = 0;

	private char SPACE = ' ';
	private char ESCAPE = '"';
	private char SEPARATOR = ':';

	private char RANGE_START = '[';
	private char RANGE_END = ']';

	private char DEPTH_START = '(';
	private char DEPTH_END = ')';

	private char OR_SEPARATOR = ',';
	private String ESCAPE_REPLACE = "^|^|^|^|";
	private String ESCAPE_REPLACE2 = "^|_|^|_|";

	private Builder queryBuilder = new BooleanQuery.Builder();
	private List<QueryInfo> queryList = new ArrayList<>();
	private Occur occur = null;

	private Map<String, ColumnInfo> columnInfoCache;
	private Map<String, ColumnInfo> columnInfoAliasCache;

	private int notQueryCount = 0;
	private int queryCount = 0;

	/** 검색 컬럼들 저장 */
	private Set<String> columnSet = new HashSet<>();

	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	public QueryBuilder(String text, Map<String, ColumnInfo> columnInfoCache) {
		String trimText = StringUtils.trim(text);

		/** 최초 NOT 으로 시작하는 구문 예외처리 */
		if (StringUtils.startsWithIgnoreCase(text, "NOT ")) {
			trimText = "AND " + trimText;
		}

		this.text = trimText;
		this.columnInfoCache = columnInfoCache;

		Map<String, ColumnInfo> columnInfoAliasCache = new LinkedHashMap<>();
		columnInfoCache.entrySet().forEach(entry -> {
			columnInfoAliasCache.put(StringUtils.lowerCase(entry.getValue().getAlias()), entry.getValue());
		});

		this.columnInfoAliasCache = columnInfoAliasCache;
	}

	public QueryBuilder(String text, int depth, Map<String, ColumnInfo> columnInfoCache) {
		this.text = StringUtils.trim(text);
		this.depth = depth;
		this.columnInfoCache = columnInfoCache;

		Map<String, ColumnInfo> columnInfoAliasCache = new LinkedHashMap<>();
		columnInfoCache.entrySet().forEach(entry -> {
			columnInfoAliasCache.put(StringUtils.lowerCase(entry.getValue().getAlias()), entry.getValue());
		});

		this.columnInfoAliasCache = columnInfoAliasCache;
	}

	public Query build() throws ParseException {
		if (StringUtils.isEmpty(text)) {
			return new MatchAllDocsQuery();
		}

		boolean notFlag = false;

		while (isNext()) {
			String token = nextToken();
			if (StringUtils.isEmpty(token)) {
				continue;
			}

			if (token.charAt(0) == DEPTH_START) {
				QueryBuilder builder = new QueryBuilder(StringUtils.substring(token, 1, -1), depth + 1, columnInfoCache);
				queryList.add(new QueryInfo(builder.build(), notFlag));

				if (notFlag) {
					notQueryCount++;
				} else {
					queryCount++;
				}

				notFlag = false;
				continue;
			}

			if (StringUtils.equalsAnyIgnoreCase(token, "AND")) {
				if (occur == null) {
					occur = Occur.MUST;
				}
				continue;
			}

			if (StringUtils.equalsAnyIgnoreCase(token, "OR")) {
				if (occur == null) {
					occur = Occur.SHOULD;
				}
				continue;
			}

			if (StringUtils.equalsAnyIgnoreCase(token, "NOT")) {
				notFlag = true;
				continue;
			}

			String[] tokenArray = splitAndTrim(token, SEPARATOR);
			queryList.add(new QueryInfo(generateQuery(tokenArray[0], tokenArray[1]), notFlag));

			if (notFlag) {
				notQueryCount++;
			} else {
				queryCount++;
			}

			notFlag = false;
		}

		if (null == occur) {
			return queryList.get(0).getQuery();

		} else {
			for (int i = 0; i < queryList.size(); i++) {
				QueryInfo queryInfo = queryList.get(i);
				Occur currentOccur = occur;

				/** not 절이 있는경우 강제 치환 */
				if (queryInfo.not) {
					currentOccur = Occur.MUST_NOT;
				}

				queryBuilder.add(queryInfo.getQuery(), currentOccur);
			}

			/** notQuery만 존재하는 경우 예외처리 */
			if (notQueryCount > 0 && queryCount == 0) {
				queryBuilder.add(new MatchAllDocsQuery(), Occur.MUST);
			}

			return queryBuilder.build();
		}
	}

	private String nextToken() {
		StringBuilder builder = new StringBuilder();

		boolean escape = false;
		boolean range = false;
		int bracket = 0;

		while (true) {
			if (text.length() <= pointer) {
				break;
			}

			char currentChar = text.charAt(pointer);
			char beforeChar = ' ';

			if (pointer > 1) {
				beforeChar = text.charAt(pointer - 1);
			}

			if (!escape && !range && bracket == 0 && currentChar == SPACE) {
				pointer++;
				break;
			}

			if (currentChar == ESCAPE) {
				if (escape) {
					if (beforeChar != '\\') {
						escape = false;
					}
				} else {
					escape = true;
				}
			}

			if (currentChar == RANGE_START && !escape) {
				range = true;
			}

			if (currentChar == RANGE_END && !escape) {
				range = false;
			}

			if (currentChar == DEPTH_START) {
				bracket++;
			}

			if (currentChar == DEPTH_END) {
				bracket--;
			}

			builder.append(currentChar);
			pointer++;
		}

		return builder.toString().trim();
	}

	private boolean isNext() {
		return !(text.length() <= pointer);
	}

	/**
	 * text를 trim
	 * 
	 * @param text
	 * @param separatorChars
	 * @param size
	 * @return String[]
	 */
	private String[] splitAndTrim(String text, char separatorChars) {
		/** 쌍따옴표 예외처리 */
		text = StringUtils.replace(text, "\\\"", ESCAPE_REPLACE2);

		String[] results = new StrTokenizer(text, separatorChars, '"').setTrimmerMatcher(StrMatcher.charMatcher(' ')).setIgnoreEmptyTokens(false).getTokenArray();

		if (results.length <= 2) {
			if (results.length > 1) {
				results[1] = StringUtils.replace(results[1], ESCAPE_REPLACE2, "\"");
			}
			return results;
		} else {
			StringJoiner joiner = new StringJoiner(Character.toString(separatorChars));
			for (int i = 1; i < results.length; i++) {
				joiner.add(results[i]);
			}

			String value = joiner.toString();
			value = StringUtils.replace(value, ESCAPE_REPLACE2, "\"");

			return new String[] { results[0], value };
		}
	}

	private String[] splitAndTrimAll(String text, char separatorChars) {
		/** 콤마문자열 처리를 위한 코드 */
		text = StringUtils.replace(text, "\\,", ESCAPE_REPLACE);

		String[] temp = new StrTokenizer(text, separatorChars, '"').setTrimmerMatcher(StrMatcher.charMatcher(' ')).setIgnoreEmptyTokens(false).getTokenArray();
		return temp;
	}

	/**
	 * String query
	 * 
	 * @param name
	 * @param value
	 * @return Query
	 */
	private Query parseForString(String name, String value) {
		value = StringUtils.lowerCase(value);
		String[] valueArray = splitAndTrimAll(value, OR_SEPARATOR);

		if (valueArray.length > 1) {
			Builder builder = new BooleanQuery.Builder();

			for (String valueSub : valueArray) {
				builder.add(parseForString(name, valueSub), Occur.SHOULD);
			}

			return builder.build();
		} else {
			value = StringUtils.replace(value, ESCAPE_REPLACE, ","); // 콤마 복원

			if (StringUtils.startsWith(value, "*") || StringUtils.endsWith(value, "*")) {
				return new WildcardQuery(new Term(name, value));
			} else if (isRange(value)) {
				List<String> range = getRange(value);
				return new TermRangeQuery(name, new BytesRef(range.get(0)), new BytesRef(range.get(1)), true, true);
			} else {
				/** 예외처리 **/
				value = StringUtils.replaceAll(value, "\\\\\"", "\"");
				value = StringUtils.replaceAll(value, "\\\\,", ",");

				return new TermQuery(new Term(name, value));
			}
		}
	}

	private Query parseForText(String name, String value) throws IOException {
		value = StringUtils.lowerCase(value);

		/** wild card query 인경우 string 쿼리 호출 */
		if (StringUtils.startsWith(value, "*") || StringUtils.endsWith(value, "*")) {
			return parseForString(name, value);
		}

		Occur occur = Occur.SHOULD;

		/** ip 타입인 경우 string 쿼리를 호출함 */
		if (StringUtils.contains(value, ':')) {
			if (InetAddresses.isInetAddress(value)) {
				occur = Occur.MUST;
			}
		}

		TsmAnalyzer tsmAnalyzer = new TsmAnalyzer();
		TokenStream stream = tsmAnalyzer.tokenStream(name, value);
		stream.reset();

		List<String> terms = new ArrayList<>();
		while (stream.incrementToken()) {
			terms.add(stream.getAttribute(TermToBytesRefAttribute.class).getBytesRef().utf8ToString());
		}
		stream.close();
		tsmAnalyzer.close();

		if (!CollectionUtils.isEmpty(terms)) {
			if (terms.size() == 1) {
				return new TermQuery(new Term(name, terms.get(0)));
			} else {
				Builder builder = new BooleanQuery.Builder();
				for (String term : terms) {
					builder.add(new TermQuery(new Term(name, term)), occur);
				}
				return builder.build();
			}
		} else {
			return new MatchAllDocsQuery();
		}
	}

	/**
	 * ip query
	 * 
	 * @param name
	 * @param value
	 * @return Query
	 * @throws UnknownHostException
	 */
	private Query parseForIp(String name, String value) throws UnknownHostException {

		String[] valueArray = splitAndTrimAll(value, OR_SEPARATOR);
		/** 콤마는 단일 아이피만 지원한다. */
		if (valueArray.length > 1) {
			List<InetAddress> addressList = new ArrayList<>();

			for (String ip : valueArray) {
				addressList.add(InetAddress.getByName(ip));
			}

			return InetAddressPoint.newSetQuery(name, addressList.toArray(new InetAddress[addressList.size()]));
		} else {
			if (isRange(value)) { // range 인 경우 처리
				List<String> range = getRange(value);
				return InetAddressPoint.newRangeQuery(name, InetAddress.getByName(range.get(0)), InetAddress.getByName(range.get(1)));
			} else {

				String[] subTexts = splitAndTrim(value, '/'); // netmask처리
				if (subTexts.length > 1) {
					return InetAddressPoint.newPrefixQuery(name, InetAddress.getByName(subTexts[0]), NumberUtils.toInt(subTexts[1], 32));
				} else {
					if (StringUtils.startsWith(value, "*") || StringUtils.endsWith(value, "*")) {
						return new WildcardQuery(new Term(name, subTexts[0]));
					} else {
						return new TermQuery(new Term(name, subTexts[0]));
					}
				}
			}
		}
	}

	private Query parseForInt(String name, String value) throws ParseException {

		int min = Integer.MIN_VALUE;
		int max = Integer.MAX_VALUE;

		try {
			if (isRange(value)) { // range 인 경우 처리
				List<String> range = getRange(value);

				min = NumberUtils.toInt(range.get(0));
				max = NumberUtils.toInt(range.get(1));

			} else {
				if (StringUtils.startsWith(value, "<=")) {
					int convertValue = NumberUtils.toInt(value.substring(2));
					max = convertValue;

				} else if (StringUtils.startsWith(value, ">=")) {
					int convertValue = NumberUtils.toInt(value.substring(2));
					min = convertValue;

				} else if (StringUtils.startsWith(value, "<")) {
					int convertValue = NumberUtils.toInt(value.substring(1));
					max = convertValue - 1;

				} else if (StringUtils.startsWith(value, ">")) {
					int convertValue = NumberUtils.toInt(value.substring(1));
					min = convertValue + 1;

				} else {
					min = NumberUtils.toInt(value);
					max = NumberUtils.toInt(value);
				}
			}

			return IntPoint.newRangeQuery(name, min, max);

		} catch (Exception e) {
			throw new ParseException("parseForInt error : " + value);
		}

	}

	private Query parseForLong(String name, String value) throws ParseException {

		long min = Long.MIN_VALUE;
		long max = Long.MAX_VALUE;

		try {
			if (isRange(value)) { // range 인 경우 처리
				List<String> range = getRange(value);

				min = NumberUtils.toLong(range.get(0));
				max = NumberUtils.toLong(range.get(1));

			} else {
				if (StringUtils.startsWith(value, "<=")) {
					long convertValue = NumberUtils.toLong(value.substring(2));
					max = convertValue;

				} else if (StringUtils.startsWith(value, ">=")) {
					long convertValue = NumberUtils.toLong(value.substring(2));
					min = convertValue;

				} else if (StringUtils.startsWith(value, "<")) {
					long convertValue = NumberUtils.toLong(value.substring(1));
					max = convertValue - 1;

				} else if (StringUtils.startsWith(value, ">")) {
					long convertValue = NumberUtils.toLong(value.substring(1));
					min = convertValue + 1;

				} else {
					min = NumberUtils.toLong(value);
					max = NumberUtils.toLong(value);
				}
			}

			return LongPoint.newRangeQuery(name, min, max);

		} catch (Exception e) {
			throw new ParseException("parseForInt error : " + value);
		}

	}

	private Query parseForDouble(String name, String value) throws ParseException {

		double min = Double.NEGATIVE_INFINITY;
		double max = Double.POSITIVE_INFINITY;

		try {
			if (isRange(value)) { // range 인 경우 처리
				List<String> range = getRange(value);

				min = NumberUtils.toDouble(range.get(0));
				max = NumberUtils.toDouble(range.get(1));

			} else {
				if (StringUtils.startsWith(value, "<=")) {
					double convertValue = NumberUtils.toDouble(value.substring(2));
					max = convertValue;

				} else if (StringUtils.startsWith(value, ">=")) {
					double convertValue = NumberUtils.toDouble(value.substring(2));
					min = convertValue;

				} else if (StringUtils.startsWith(value, "<")) {
					double convertValue = NumberUtils.toDouble(value.substring(1));
					max = DoublePoint.nextDown(convertValue);

				} else if (StringUtils.startsWith(value, ">")) {
					double convertValue = NumberUtils.toDouble(value.substring(1));
					min = DoublePoint.nextUp(convertValue);

				} else {
					min = NumberUtils.toDouble(value);
					max = NumberUtils.toDouble(value);
				}
			}

			return DoublePoint.newRangeQuery(name, min, max);

		} catch (Exception e) {
			throw new ParseException("parseForInt error : " + value);
		}

	}

	private Query parseForFloat(String name, String value) throws ParseException {

		float min = Float.MIN_VALUE;
		float max = Float.MAX_VALUE;

		try {
			if (isRange(value)) { // range 인 경우 처리
				List<String> range = getRange(value);

				min = NumberUtils.toFloat(range.get(0));
				max = NumberUtils.toFloat(range.get(1));

			} else {
				if (StringUtils.startsWith(value, "<=")) {
					float convertValue = NumberUtils.toFloat(value.substring(2));
					max = convertValue;

				} else if (StringUtils.startsWith(value, ">=")) {
					float convertValue = NumberUtils.toFloat(value.substring(2));
					min = convertValue;

				} else if (StringUtils.startsWith(value, "<")) {
					float convertValue = NumberUtils.toFloat(value.substring(1));
					max = convertValue - 1;

				} else if (StringUtils.startsWith(value, ">")) {
					float convertValue = NumberUtils.toFloat(value.substring(1));
					min = convertValue + 1;

				} else {
					min = NumberUtils.toFloat(value);
					max = NumberUtils.toFloat(value);
				}
			}

			return FloatPoint.newRangeQuery(name, min, max);

		} catch (Exception e) {
			throw new ParseException("parseForInt error : " + value);
		}

	}

	private Query parseForDateTime(String name, String value) throws UnknownHostException {
		ZonedDateTime before;
		ZonedDateTime after;

		if (isRange(value)) { // range 인 경우 처리
			List<String> range = getRange(value);

			String valueText1 = range.get(0);
			String valueText2 = range.get(1);

			if (valueText1.length() == 10) {
				valueText1 += " 00:00:00";
			} else if (valueText1.length() == 13) {
				valueText1 += ":00:00";
			} else if (valueText1.length() == 16) {
				valueText1 += ":00";
			}

			if (valueText2.length() == 10) {
				valueText2 += " 00:00:00";
			} else if (valueText2.length() == 13) {
				valueText2 += ":00:00";
			} else if (valueText2.length() == 16) {
				valueText2 += ":00";
			}

			before = ZonedDateTime.parse(valueText1, DATE_TIME_FORMATTER);
			after = ZonedDateTime.parse(valueText2, DATE_TIME_FORMATTER);

		} else {

			String valueText = value;
			if (value.length() == 10) {
				valueText += " 00:00:00";

				before = ZonedDateTime.parse(valueText, DATE_TIME_FORMATTER);
				after = before.plusDays(1L);
			} else if (value.length() == 13) {

				valueText += ":00:00";

				before = ZonedDateTime.parse(valueText, DATE_TIME_FORMATTER);
				after = before.plusHours(1L);
			} else if (value.length() == 16) {

				valueText += ":00";

				before = ZonedDateTime.parse(valueText, DATE_TIME_FORMATTER);
				after = before.plusMinutes(1L);
			} else {

				before = ZonedDateTime.parse(valueText, DATE_TIME_FORMATTER);
				after = before.plusSeconds(1L);
			}

		}

		after = after.minusNanos(1L);
		if (after.isAfter(ZonedDateTime.now(ZoneId.systemDefault()))) {
			after = ZonedDateTime.now(ZoneId.systemDefault());
		}

		return LongPoint.newRangeQuery(name, before.toInstant().toEpochMilli(), after.toInstant().toEpochMilli());
	}

	/**
	 * 시작이 "[" 끝이 "]" 인 경우 range 로 인식한다.
	 * 
	 * @param text
	 * @return boolean
	 */
	private boolean isRange(String text) {
		if (StringUtils.startsWith(text, "[") && StringUtils.endsWith(text, "]")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * range 인 경우 start, end 를 가져온다.
	 * 
	 * @param text
	 * @return List<String>
	 */
	private List<String> getRange(String text) {
		String text2 = StringUtils.substring(text, 1, -1);
		String[] token = new StrTokenizer(text2, ' ', '"').setTrimmerMatcher(StrMatcher.charMatcher(' ')).getTokenArray();

		if (token.length == 3) {
			return Arrays.asList(token[0], token[2]);
		} else if (token.length == 5) {
			return Arrays.asList(token[0] + " " + token[1], token[3] + " " + token[4]);
		} else {
			return null;
		}
	}

	/**
	 * 쿼리를 생성한다.
	 * 
	 * @param key
	 * @param value
	 * @return Query
	 * @throws ParseException
	 */
	private Query generateQuery(String name, String value) throws ParseException {
		ColumnInfo columnInfo = null;

		/** device, type, address 는 예외처리함. */
		if (StringUtils.equalsIgnoreCase(name, "address")) {
			columnInfo = new ColumnInfo("_address", LogFieldType.IP, true, true);
		} else if (StringUtils.equalsIgnoreCase(name, "size")) {
			columnInfo = new ColumnInfo("_size", LogFieldType.LONG, true, true);
		} else if (StringUtils.equalsIgnoreCase(name, "message")) {
			columnInfo = new ColumnInfo("_message", LogFieldType.TEXT, true, false);
		} else {
			columnInfo = columnInfoCache.get(name);

			/** alias 처리 */
			if (null == columnInfo) {
				columnInfo = columnInfoAliasCache.get(StringUtils.lowerCase(name));
			}
		}

		if (null == columnInfo) {
			throw new ParseException("columnInfo is not exist : " + name);
		}

		Query query = null;
		columnSet.add(columnInfo.getName());

		try {
			switch (columnInfo.getLogFieldType()) {
			case IP:
				query = parseForIp(columnInfo.getName(), value);
				break;

			case INT:
				query = parseForInt(columnInfo.getName(), value);
				break;

			case LONG:
				query = parseForLong(columnInfo.getName(), value);
				break;

			case DOUBLE:
				query = parseForDouble(columnInfo.getName(), value);
				break;

			case FLOAT:
				query = parseForFloat(columnInfo.getName(), value);
				break;

			case DATETIME:
				query = parseForDateTime(columnInfo.getName(), value);
				break;

			case NONE:
				break;

			case TEXT:
				query = parseForText(columnInfo.getName(), value);
				break;

			case STRING:
			case COUNTRY:
			default:
				query = parseForString(columnInfo.getName(), value);
				break;
			}

		} catch (Exception e) {
			throw new ParseException("parse error : " + e.getMessage());
		}

		return query;
	}

	public Set<String> getColumnSet() {
		return columnSet;
	}

	/**
	 * 쿼리 저장소
	 */
	public class QueryInfo {
		private Query query;
		private boolean not;

		public QueryInfo(Query query, boolean not) {
			this.query = query;
			this.not = not;
		}

		public Query getQuery() {
			return query;
		}

		public void setQuery(Query query) {
			this.query = query;
		}

		public boolean isNot() {
			return not;
		}

		public void setNot(boolean not) {
			this.not = not;
		}

	}
}
