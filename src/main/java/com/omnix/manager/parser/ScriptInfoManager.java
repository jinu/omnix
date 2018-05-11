package com.omnix.manager.parser;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.transaction.Transactional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.omnix.manager.repository.ScriptInfoRepository;

@Service
public class ScriptInfoManager {
	@Autowired
	private ScriptInfoRepository scriptInfoRepository;

	private String globalScript;

	/** tableschemaid_scriptid : value */
	public static final Map<Long, Map<Long, ScriptInfo>> SCRIPTINFO_CACHE = new ConcurrentHashMap<>();

	/** {tableId_scriptName : scriptId} */
	public static LoadingCache<String, Long> NAME_CACHE;

	/** Logger */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public void init() {
		TableSchemaManager.getListCache().forEach(tableSchema -> {
			SCRIPTINFO_CACHE.put(tableSchema.getId(), new ConcurrentHashMap<>());
		});

		/** globalscript loading */
		try (InputStream is = ResourceUtils.getURL("classpath:etc/global.js").openStream()) {
			this.globalScript = IOUtils.toString(is, "UTF-8");

		} catch (Exception e) {
			logger.error("failed to globa.js load", e);
		}

		/** cache에 올림 **/
		scriptInfoRepository.findAll().forEach(scriptInfo -> {
			try {
				Invocable invocable = getInvocable(scriptInfo.getScript());
				scriptInfo.setInvocable(invocable);

				SCRIPTINFO_CACHE.get(scriptInfo.getTableSchema().getId()).put(scriptInfo.getId(), scriptInfo);

				logger.info("script loading : {} => {}", scriptInfo.getTableSchema().getName(), scriptInfo.getName());
			} catch (ScriptException e) {
				logger.error("init script error", e);
			}
		});

		/** nameCache 처리 */
		NAME_CACHE = CacheBuilder.newBuilder().build(CacheLoader.<String, Long>from(key -> {
			String[] temp = StringUtils.split(key, "_", 2);
			Map<Long, ScriptInfo> map = SCRIPTINFO_CACHE.get(NumberUtils.toLong(temp[0]));

			if (map == null) {
				return null;
			}

			for (ScriptInfo scriptInfo : map.values()) {
				if (StringUtils.equals(scriptInfo.getName(), temp[1])) {
					return scriptInfo.getId();
				}
			}

			return null;
		}));
	}

	public static List<ScriptInfo> getScriptInfoCache(TableSchema tableSchema) {
		Map<Long, ScriptInfo> temp = SCRIPTINFO_CACHE.get(tableSchema.getId());
		if (null == temp) {
			return Arrays.asList();
		}

		return temp.values().stream().sorted(Comparator.comparingLong(ScriptInfo::getId)).collect(Collectors.toList());
	}

	public static ScriptInfo getScriptInfoCache(long tableId, long scriptId) {
		Map<Long, ScriptInfo> temp = SCRIPTINFO_CACHE.get(tableId);
		if (null == temp) {
			return null;
		}

		return temp.get(scriptId);
	}

	public static ScriptInfo getScriptInfoCache(long tableId, String scriptName) {
		Map<Long, ScriptInfo> temp = SCRIPTINFO_CACHE.get(tableId);
		if (null == temp) {
			return null;
		}

		String key = new StringBuilder().append(tableId).append("_").append(scriptName).toString();
		try {
			Long id = NAME_CACHE.get(key);
			if (null == id) {
				return null;
			}
			
			return getScriptInfoCache(tableId, id);

		} catch (ExecutionException e) {
			return null;
		}
	}

	public Optional<ScriptInfo> getScriptInfo(long id) {
		return scriptInfoRepository.findById(id);
	}

	public ScriptInfo getScriptInfoForUpdate(long id) {
		return scriptInfoRepository.getOne(id);
	}

	@Transactional
	public ScriptInfo saveScriptInfo(ScriptInfo scriptInfo) throws ScriptException {
		if (scriptInfo.getId() == 0L) {
			scriptInfoRepository.saveAndFlush(scriptInfo);
			
		} else {
			ScriptInfo oldScriptInfo = scriptInfoRepository.getOne(scriptInfo.getId());
			
			oldScriptInfo.setName(scriptInfo.getName());
			oldScriptInfo.setScript(scriptInfo.getScript());
			oldScriptInfo.setModifyDate(LocalDateTime.now());
			oldScriptInfo.setDescription(scriptInfo.getDescription());
			
			scriptInfo = scriptInfoRepository.saveAndFlush(oldScriptInfo);
		}

		Invocable invocable = getInvocable(scriptInfo.getScript());
		scriptInfo.setInvocable(invocable);

		SCRIPTINFO_CACHE.get(scriptInfo.getTableSchema().getId()).put(scriptInfo.getId(), scriptInfo);
		NAME_CACHE.cleanUp();

		return scriptInfo;
	}

	public void deleteScriptInfo(ScriptInfo scriptInfo) {
		scriptInfoRepository.delete(scriptInfo);
		SCRIPTINFO_CACHE.get(scriptInfo.getTableSchema().getId()).remove(scriptInfo.getId());
		NAME_CACHE.cleanUp();
	}

	public Invocable getInvocable(String script) throws ScriptException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("Nashorn");

		String temp = script + globalScript;
		engine.eval(temp);

		Invocable invocable = (Invocable) engine;
		return invocable;
	}

	public static String getScriptName(long tableId, long scriptId) {
		ScriptInfo scriptInfo = getScriptInfoCache(tableId, scriptId);

		if (null != scriptInfo) {
			return scriptInfo.getName();
		} else {
			return "";
		}

	}
}
