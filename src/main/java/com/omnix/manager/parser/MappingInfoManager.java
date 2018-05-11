package com.omnix.manager.parser;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.omnix.manager.repository.MappingInfoRepository;

@Service
public class MappingInfoManager {
	@Autowired
	private MappingInfoRepository mappingInfoRepository;

	/** mapping cache */
	public static final Map<String, Map<String, String>> MAPPING_CACHE = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		mappingInfoRepository.findAll().forEach(mappingInfo -> {
			String key = mappingInfo.getName();
			MAPPING_CACHE.put(key, generateMapping(mappingInfo.getContent()));
		});
	}

	public List<MappingInfo> getMappingInfoList() {
		return mappingInfoRepository.findAll(new Sort(Sort.Direction.ASC, "id"));
	}

	public Optional<MappingInfo> getMappingInfo(long id) {
		return mappingInfoRepository.findById(id);
	}

	public MappingInfo getMappingInfoForUpdate(long id) {
		return mappingInfoRepository.getOne(id);
	}

	public MappingInfo saveMappingInfo(MappingInfo mappingInfo) {
		if (mappingInfo.getId() == 0L) {
			mappingInfoRepository.saveAndFlush(mappingInfo);
		} else {
			MappingInfo oldMappingInfo = mappingInfoRepository.getOne(mappingInfo.getId());
			
			oldMappingInfo.setContent(mappingInfo.getContent());
			oldMappingInfo.setModifyDate(LocalDateTime.now());
			oldMappingInfo.setDescription(mappingInfo.getDescription());
			
			mappingInfo = mappingInfoRepository.saveAndFlush(oldMappingInfo);
		}
		
		MAPPING_CACHE.put(mappingInfo.getName(), generateMapping(mappingInfo.getContent()));

		return mappingInfo;
	}

	public void deleteMappingInfo(MappingInfo mappingInfo) {
		mappingInfoRepository.delete(mappingInfo);
		MAPPING_CACHE.remove(mappingInfo.getName());
	}
	
	/** script에서 mapping을 위해 호출 하는 함수 */
    public static String mapping(String name, String key, String defaultValue) {
    	if (null == key) {
    		return defaultValue;
    	}
    	
        Map<String, String> map = MAPPING_CACHE.get(name);
        if (null != map) {
            String value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }
    
    public static String mapping2(String name, String key1, String key2, String defaultValue) {
        return mappingMulti(name, defaultValue, key1, key2);
    }
    
    public static String mapping3(String name, String key1, String key2, String key3, String defaultValue) {
        return mappingMulti(name, defaultValue, key1, key2, key3);
    }
    
    public static String mappingMulti(String name, String defaultValue, String...keys) {
        Map<String, String> map = MAPPING_CACHE.get(name);
        if (null != map) {
        	
        	for (String key : keys) {
        		String value = map.get(key);
        		if (StringUtils.isEmpty(value)) {
        			continue;
        		}
        		
        		if (value != null) {
                    return value;
                }
			}
        }
        return defaultValue;
    }

	public static Map<String, String> generateMapping(String content) {
		Map<String, String> map = new LinkedHashMap<>();

		if (StringUtils.isEmpty(content)) {
			return map;
		}

		String lines[] = content.split("\\r?\\n");
		for (String text : lines) {
			if (StringUtils.startsWith(text, "#")) {
				continue;
			}

			String[] temps = StringUtils.split(text, "=", 2);
			if (temps.length == 2) {
				map.put(temps[0], temps[1]);
			}
		}

		return map;
	}

}
