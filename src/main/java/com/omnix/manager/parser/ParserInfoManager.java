package com.omnix.manager.parser;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.omnix.config.UiValidationException;
import com.omnix.manager.repository.ParserInfoRepository;

@Service
public class ParserInfoManager {
	@Autowired
	private ParserInfoRepository parserInfoRepository;

	/** {ip : scriptInfoId} */
	private volatile Map<String, Long> parserInfoCache = new ConcurrentHashMap<>();
	/** Logger */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public void init() {
		reloadAll();
	}

	public void reloadAll() {
		Map<String, Long> map = new ConcurrentHashMap<>();

		getPaserInfoList().forEach(parserInfo -> {
			try {
				parseIp(parserInfo.getIp()).forEach(ip -> {
					map.put(ip, parserInfo.getScriptInfo().getId());
				});

			} catch (UnknownHostException e) {
				logger.error("ip address read fail", e);
			}
		});

		this.parserInfoCache = map;
	}
	
	public List<ParserInfo> getPaserInfoListByScriptId(long tableId, long scriptId) {
		return parserInfoRepository.findAllByScriptInfoIdAndScriptInfoTableSchemaId(tableId, scriptId, new Sort(Sort.Direction.ASC, "id"));
	}

	public List<ParserInfo> getPaserInfoListByTableId(long tableId) {
		return parserInfoRepository.findAllByScriptInfoTableSchemaId(tableId, new Sort(Sort.Direction.ASC, "id"));
	}

	public List<ParserInfo> getPaserInfoList() {
		return parserInfoRepository.findAll(new Sort(Sort.Direction.ASC, "id"));
	}

	public Optional<ParserInfo> getParserInfo(long id) {
		return parserInfoRepository.findById(id);
	}

	public ParserInfo getParserInfoForUpdate(long id) {
		return parserInfoRepository.getOne(id);
	}

	public ParserInfo saveParserInfo(ParserInfo parserInfo) {
		parserInfo = parserInfoRepository.saveAndFlush(parserInfo);
		reloadAll();
		return parserInfo;
	}

	public void deleteParserInfo(ParserInfo parserInfo) {
		parserInfoRepository.delete(parserInfo);
		reloadAll();
	}

	public boolean existIp(String ip) {
		return parserInfoCache.containsKey(ip);
	}

	public List<String> parseIp(String ipTexts) throws UnknownHostException {
		List<String> ipList = new ArrayList<>();

		List<String> texts = new ArrayList<>();
		for (String ip : StringUtils.split(ipTexts, ",")) {
			texts.add(StringUtils.trim(ip));
		}

		for (String ip : texts) {
			ip = StringUtils.trim(ip);
			if (StringUtils.isEmpty(ip)) {
				continue;
			}

			if (StringUtils.contains(ip, "/")) {
				SubnetUtils utils = new SubnetUtils(StringUtils.trim(ip));
				utils.setInclusiveHostCount(true);

				for (String subnetIp : utils.getInfo().getAllAddresses()) {
					ipList.add(subnetIp);
				}
			} else if (StringUtils.contains(ip, "-")) {
				String[] ipRange = StringUtils.split(ip, "-");
				BigInteger startBigInteger = new BigInteger(InetAddress.getByName(StringUtils.trim(ipRange[0])).getAddress());
				BigInteger endBigInteger = new BigInteger(InetAddress.getByName(StringUtils.trim(ipRange[1])).getAddress());

				for (int i = 0; i <= 10000; i++) { // 최대 1000개만 돔
					BigInteger current = startBigInteger.add(BigInteger.valueOf(i));
					ipList.add(InetAddress.getByAddress(current.toByteArray()).getHostAddress());

					if (endBigInteger.compareTo(current) == 0) {
						break;
					}

					if (i == 10000) {
						throw new UiValidationException("max 1000 limit error");
					}
				}

			} else {
				ipList.add(InetAddress.getByName(StringUtils.trim(ip)).getHostAddress());
			}
		}

		if (ipList.size() > 10000) {
			throw new UiValidationException("max 1000 limit error");
		}
		return ipList;
	}
	
	public long getScriptId(String ip) {
		return parserInfoCache.get(ip);
	}
}
