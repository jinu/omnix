package com.omnix.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.text.StrMatcher;
import org.apache.commons.text.StrTokenizer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Splitter;
import com.omnix.manager.parser.DocumentMapper;

public class ParseUtils {
	private static final StrMatcher SPACE_MATCHER = StrMatcher.charMatcher(' ');
	private static final Map<String, DateTimeFormatter> cache = new HashMap<>();
	
	private static final TypeReference<Map<String, String>> jsonType1 = new TypeReference<Map<String, String>>() {};
	private static final TypeReference<Map<String, Object>> jsonType2 = new TypeReference<Map<String, Object>>() {};
	private static final JsonFactory JSON_FACTORY = new JsonFactory();
	
	public static final DateTimeFormatter DEFAULT_APACHE_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss ZZ", Locale.ENGLISH);
	
	public static String escapeQueryValue(String queryValue) {
    	String[] searchList = new String[] {"+", "-" ,"&&", "||", "!", "(",  ")", "{",  "}",  "[", "]", "^", "\"", "~" , "*" , "?" , ":" , "\\"};
		String[] replacementList = new String[] {"\\+", "\\-" ,"\\&&", "\\||", "\\!", "\\(",  "\\)", "\\{",  "\\}",  "\\[", "\\]", "\\^", "\\\"", "\\~" , "\\*" , "\\?" , "\\:" , "\\\\"};
		String result = StringUtils.replaceEach(queryValue, searchList, replacementList);
    	return result;
    }

	/**
	 * trim 및 쌍따옴표로 감싸진 text처리 ("asdf " => asdf)
	 * 
	 * @param text
	 * @return trim and remove double quotes text
	 */
	public static String trimString(String text) {
		if (text.charAt(0) == '"') {
			text = text.substring(1, text.length() - 1);
		}

		return text.trim();
	}

	/**
	 * 해당 함수는 split 보다 성능이 느리지만 trim 및 쌍따옴표 예외처리를 해줌.
	 * 
	 * @param text
	 * @return String[]
	 */
	public static String[] splitComma(String text) {
		return new StrTokenizer(text, ',', '"').setTrimmerMatcher(SPACE_MATCHER).getTokenArray();
	}

	/**
	 * 해당 함수는 split 보다 성능이 느리지만 trim 및 쌍따옴표 예외처리를 해줌.
	 * 
	 * @param text
	 * @return String[]
	 */
	public static String[] splitSpace(String text) {
		return new StrTokenizer(text, ' ', '"').setTrimmerMatcher(SPACE_MATCHER).getTokenArray();
	}

	/** time stamp */
	public static long makeTimeStamp(LocalDateTime localDateTime) {
		return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	/**
	 * key=value key2=value2 to {key:value, key2:value2}<br />
	 * 
	 * @param text
	 * @param delimChar
	 * @param seperatorChar
	 * @return Map<String, String>
	 */
	public static Map<String, String> splitToMap(String text, char delimChar, char seperatorChar) {
		return Splitter.on(delimChar).trimResults().withKeyValueSeparator(Splitter.on(seperatorChar).trimResults()).split(text);
	}

	/**
	 * key=value key2=value2 to {key:value, key2:value2}<br />
	 * escape 처리가 없는 위 함수에 비해 2~30%정도 성능이 느림.
	 * 
	 * @param delimeter
	 * @param escapeChar
	 * @param seperatorChar
	 * @param text
	 * @return Map<String, String>
	 */
	public static Map<String, String> splitToMap(String text, char delimChar, char seperatorChar, char escapeChar) {
		Map<String, String> resultMap = new LinkedHashMap<>();

		int size = text.length();
		StringBuilder builder = new StringBuilder();
		boolean escapeFlag = false;
		boolean isKey = true;
		String currentKey = "";
		for (int i = 0; i < size; i++) {
			char current = text.charAt(i);
			int builderSize = builder.length();

			if (builderSize == 0 && current == delimChar) {
				continue;
			}

			if (builderSize == 0 && current == escapeChar && !isKey) {
				escapeFlag = true;
				continue;
			}

			if (escapeFlag && current == escapeChar && !isKey) {
				if (i + 1 <= size && text.charAt(i + 1) == delimChar) {
					escapeFlag = false;
					continue;
				}
			}

			if (current == delimChar && !escapeFlag) {
				isKey = true;

				if (!StringUtils.isEmpty(currentKey) && builderSize > 0) {
					resultMap.put(currentKey, builder.toString());
				}

				currentKey = "";
				builder = new StringBuilder();
				continue;
			}

			if (current == seperatorChar) {
				isKey = false;
				currentKey = builder.toString();
				builder = new StringBuilder();
				continue;
			}

			builder.append(current);
		}

		if (!StringUtils.isEmpty(currentKey) && builder.length() > 0) {
			resultMap.put(currentKey, builder.toString());
		}

		return resultMap;
	}

	/**
	 * key=value key2=http://test?id=1 to {key:value, key2:value2}<br />
	 * 
	 * @param text
	 * @param delimChar
	 * @param seperatorChar
	 * @return Map<String, String>
	 */
	public static Map<String, String> splitToMap(String text, String delimChar, String seperatorChar) {
		String[] textArray1 = text.split(delimChar);
		Map<String, String> map = new HashMap<>();
		Stream.of(textArray1).filter(x -> x.indexOf("=") > 0).forEach(textValue -> {
			String[] valueArray = textValue.split(seperatorChar, 2);
			map.put(valueArray[0], valueArray[1]);
		});

		return map;
	}

	public static String appendString(String... text) {
		StringBuilder builder = new StringBuilder();
		for (String string : text) {
			if (!StringUtils.isEmpty(string)) {
				builder.append(string);
			}
		}

		return builder.toString();
	}

	public static int toInt(String text) {
		return NumberUtils.toInt(text);
	}

	public static long toLong(String text) {
		return NumberUtils.toLong(text);
	}

	public static String plusInt(String... text) {
		int value = 0;
		for (String str : text) {
			value += NumberUtils.toInt(str);
		}
		return value + "";
	}

	public static String plusLong(String... text) {
		long value = 0;
		for (String str : text) {
			value += NumberUtils.toLong(str);
		}
		return value + "";
	}

	public static String plusDouble(String... text) {
		double value = 0;
		for (String str : text) {
			value += NumberUtils.toDouble(str);
		}
		return value + "";
	}

	public static String dateToLong(String text, String pattern) {
		String result = String.valueOf(stringToDate(text, pattern).toInstant().toEpochMilli());
		return result;
	}

	public static ZonedDateTime stringToDate(String text, String pattern) {
		DateTimeFormatter dateTimeFormatter = cache.get(pattern);
		if (null == dateTimeFormatter) {
			dateTimeFormatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
			cache.put(pattern, dateTimeFormatter);
		}

		return ZonedDateTime.parse(text, dateTimeFormatter);
	}

	public static ZonedDateTime stringToDateISO(String text) {
		return ZonedDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME);
	}

	public static String getCountryCode(String ip) {
		return DocumentMapper.getCountryCode(ip);
	}
	
	public static ZonedDateTime stringToDate(String text) {
		return ZonedDateTime.parse(text);
	}
	
	public static ZonedDateTime stringToDateApache(String text) {
		return ZonedDateTime.parse(text, DEFAULT_APACHE_DATE_TIME_PATTERN);
	}
	
	public static ZonedDateTime stringToDate(String text, int offset) {
		return ZonedDateTime.parse(text).toInstant().atOffset(ZoneOffset.ofHours(offset)).toZonedDateTime();
	}

	public static boolean checkIp(String text) {
		text = StringUtils.trim(text);

		if (StringUtils.isEmpty(text)) {
			return false;
		}

		if (StringUtils.contains(text, "-")) {
			String[] temp = StringUtils.splitPreserveAllTokens(text, "-");

			if (temp.length != 2) {
				return false;
			}

			boolean flag1 = checkIp(temp[0]);
			boolean flag2 = checkIp(temp[1]);

			if (flag1 && flag2) {
				return true;
			} else {
				return false;
			}

		} else {
			if (!StringUtils.contains(text, "/")) {
				text = text + "/32";
			}

			try {
				new SubnetUtils(text);
			} catch (Exception e) {
				return false;
			}

			return true;
		}
	}

	public static List<String> getIpList(String ip) throws Exception {
		List<String> ipList = new ArrayList<>();

		if (StringUtils.contains(ip, "/")) {
			SubnetUtils utils = new SubnetUtils(StringUtils.trim(ip));
			utils.setInclusiveHostCount(true);

			for (String subnetIp : utils.getInfo().getAllAddresses()) {
				ipList.add(subnetIp);
			}
		} else if (StringUtils.contains(ip, "-")) {
			String[] ipRange = StringUtils.splitPreserveAllTokens(ip, "-");

			BigInteger startBigInteger = new BigInteger(InetAddress.getByName(StringUtils.trim(ipRange[0])).getAddress());
			BigInteger endBigInteger = new BigInteger(InetAddress.getByName(StringUtils.trim(ipRange[1])).getAddress());

			for (int i = 0; i <= 10000; i++) { // 최대 10000개만 돔
				BigInteger current = startBigInteger.add(BigInteger.valueOf(i));
				ipList.add(InetAddress.getByAddress(current.toByteArray()).getHostAddress());

				if (endBigInteger.compareTo(current) == 0) {
					break;
				}

				if (i == 10000) {
					throw new RuntimeException("max 10000 limit error");
				}
			}
		} else {
			ipList.add(InetAddress.getByName(StringUtils.trim(ip)).getHostAddress());
		}

		return ipList;
	}
	
	/**
	 * LEEF 포멧 로그 체크
	 * 
	 * @param text 로그
	 * @return 체크 결과(true=LEEF 포멧)
	 */
	public static boolean isLeefFormat(String text) {

        if (text == null || text.length() < 4) {
            return false;
        }
		
		if (StringUtils.startsWith(text, "LEEF")) {
			return true;
		}

		return false;
	}
    
    /**
     * LEEF 포멧 로그 파싱
     * @param text 로그
     * @return map형식으로 파싱된 결과
     */
    public static Map<String, String> parseLeefLog(String text) {
    	
        Map<String, String> map = new HashMap<>();
        
		String[] texts = StringUtils.splitPreserveAllTokens(text, "|");
		String version = texts[0];
		String vendor = texts[1]; 
		String productName = texts[2];
		String productVersion = texts[3];
		String eventId = texts[4];
		String delimiter = StringUtils.defaultIfBlank(texts[5], ",");
		String data = texts[6];
		
		// Header data
		map.put("h_version", version);
		map.put("h_vendor", vendor);
		map.put("h_product_name", productName);
		map.put("h_product_version", productVersion);
		map.put("h_event_id", eventId);
		map.put("h_delimiter", delimiter);
		
		// Event data
		Map<String, String> dataMap = splitToMap(data, delimiter.charAt(0), '=');
		map.putAll(dataMap);
    	
    	return map;
    }
    
    public static String numericToString(Object numericValue) {
    	if (numericValue instanceof String) {
    		return (String) numericValue;
    	}
    	
    	return String.valueOf(numericValue);
    }
    
    /**
     * 1depth 구조의 JSON 
     */
    public static Map<String, String> parseJson(String text) throws JsonParseException, JsonMappingException, IOException {
    	return JsonUtils.toObj(text, jsonType1);
    }
    
    /**
     * Ndepth 구조의 JSON 
     */
    public static Map<String, Object> parseJsonAll(String text) throws JsonParseException, JsonMappingException, IOException {
    	return JsonUtils.toObj(text, jsonType2);
    }
    
    public static Map<String, String> parseJsonFaster(String text) throws JsonParseException, IOException {
    	Map<String, String> resultMap = new HashMap<>();
    	
    	JsonParser jsonParser = JSON_FACTORY.createParser(text);
    	
    	jsonParser.nextToken();
    	while(jsonParser.nextToken() != JsonToken.END_OBJECT) {
    		String key = jsonParser.getText();
    		
    		jsonParser.nextToken();
    		String value = jsonParser.getText();
    		
    		if (!StringUtils.isBlank(value)) {
    			resultMap.put(key, value);
    		}
    	}
    	jsonParser.close();
    	
    	return resultMap;
    }
    
    public static Map<String, String> parseJsonFaster(String text, Map<String, String> result) throws JsonParseException, IOException {
    	JsonParser jsonParser = JSON_FACTORY.createParser(text);
    	
    	jsonParser.nextToken();
    	while(jsonParser.nextToken() != JsonToken.END_OBJECT) {
    		String key = jsonParser.getText();
    		
    		jsonParser.nextToken();
    		String value = jsonParser.getText();
    		
    		if (!StringUtils.isBlank(value)) {
    			result.put(key, value);
    		}
    	}
    	jsonParser.close();
    	
    	return result;
    }
    
    public static String urlDecode(String text) throws UnsupportedEncodingException {
    	if (StringUtils.isBlank(text)) {
    		return "";
    	}
    	
    	return URLDecoder.decode(text, "UTF-8" );
    }
}
