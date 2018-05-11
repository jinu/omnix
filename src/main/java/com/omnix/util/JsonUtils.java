package com.omnix.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
	private static ObjectMapper objectMapper = null;

	public static <T> T toObj(InputStream jsonTextStream, TypeReference<?> valueTypeRef) {
		ObjectMapper mapper = getMapper();
		try {
			return mapper.readValue(jsonTextStream, valueTypeRef);
		} catch (IOException e) {
			return null;
		}
	}

	public static <T> T toObj(InputStream jsonTextStream, Class<T> valueType) {
		ObjectMapper mapper = getMapper();
		try {
			return mapper.readValue(jsonTextStream, valueType);
		} catch (IOException e) {
			return null;
		}
	}

	public static <T> T toObj(Path path, TypeReference<?> valueTypeRef) {
		ObjectMapper mapper = getMapper();
		try {
			return mapper.readValue(path.toFile(), valueTypeRef);
		} catch (IOException e) {
			return null;
		}
	}

	public static <T> T toObj(String text, Class<T> valueType) {
		ObjectMapper mapper = getMapper();
		try {
			return mapper.readValue(text, valueType);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * ex) JsonUtils.toObj(text, new TypeReference<Map<String, Object>>() { });
	 */
	public static <T> T toObj(String text, TypeReference<?> valueTypeRef)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = getMapper();
		return mapper.readValue(text, valueTypeRef);
	}

	public static String fromObj(Object value) {
		ObjectMapper mapper = getMapper();

		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	public static String fromObjInline(Object value) {
		ObjectMapper mapper = getMapper();

		try {
			return mapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	public static void writeObj(Path path, Object value, boolean append) throws IOException {
		String text = fromObj(value);
		if (!StringUtils.isEmpty(text)) {
			FileUtils.write(path.toFile(), text, Charset.forName("UTF-8"), append);
		}
	}

	public static Map<String, String> toMap(String text) {
		Map<String, String> resultMap = null;
		try {
			resultMap = JsonUtils.toObj(text, new TypeReference<Map<String, String>>() {
			});
		} catch (IOException e) {
		}
		return resultMap;
	}

	public static ObjectMapper getMapper() {
		if (objectMapper == null) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.findAndRegisterModules();
			mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
			mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);

			objectMapper = mapper;
		}

		return objectMapper;
	}
}
