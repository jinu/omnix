package com.omnix.manager.generator;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.omnix.manager.LogManager;
import com.omnix.manager.recieve.LogBean;
import com.omnix.manager.repository.GeneratorInfoRepository;

@Component
public class GeneratorManager {
	@Autowired
	private GeneratorInfoRepository generatorInfoRepository;

	private boolean runFlag;

	private long tableId;
	private long scriptId;
	private List<String> contents;

	@Scheduled(fixedDelay = 1000, initialDelay = 1000)
	public void generate() {
		if (!runFlag) {
			return;
		}

		for (String text : contents) {

			/** 지정된 String 을 random 값으로 치환 */
			String randomIp = GeneratorUtil.makeRandomIp(1, 1, 10, 255);
			String randomPort = GeneratorUtil.makeRandomPort();

			text = StringUtils.replace(text, "%RAND_IP%", randomIp);
			text = StringUtils.replace(text, "%RAND_PORT%", randomPort);

			LogBean logBean = new LogBean(tableId, scriptId, text, "127.0.0.1");
			logBean.setReceiveDate(ZonedDateTime.now());

			LogManager.sendLog(logBean);
		}
	}

	public boolean run(long tableId, long scriptId, long id) {
		if (runFlag) {
			return false;
		}

		this.tableId = tableId;
		this.scriptId = scriptId;

		List<String> contents = new ArrayList<>();
		generatorInfoRepository.findById(id).ifPresent(t -> {
			String temp = t.getContent();

			String[] temps = StringUtils.splitPreserveAllTokens(temp, "\n");
			for (String text : temps) {
				if (StringUtils.isEmpty(text) || StringUtils.startsWith(text, "#")) {
					continue;
				}

				contents.add(text);
			}

			this.contents = contents;
			this.runFlag = true;
		});

		return this.runFlag;
	}

	public void stop() {
		this.runFlag = false;
	}

	public boolean isRun() {
		return runFlag;
	}

	public void setRun(boolean run) {
		this.runFlag = run;
	}

}
