package com.omnix.manager.recieve;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.omnix.manager.parser.ScriptInfoManager;
import com.omnix.manager.parser.TableSchemaManager;

/**
 * 해석된 로그가 담길 공간
 * 
 * @author seokwon
 *
 */
public class LogBean {
	private long tableId;
	private long scriptId;
	
	/** ip */
	private String remoteAddr;

	/** 기본검색에 반영될 text */
	private final List<Integer> messageList = new ArrayList<>();

	/** key, value 목록들 */
	private final List<String> keyList = new ArrayList<>();
	private final List<String> valueList = new ArrayList<>();
	private int slot = 0;

	/** 원본 text */
	private String text;
	/** 수신 Date */
	private ZonedDateTime receiveDate;
	/** 수신 timestamp */
	private long timestamp;

	private boolean parseComplete;

	/** true인 경우 indexing 처리중인 구조체임을 알림 */
	private boolean indexFlag;

	/** 해당 값이 존재하면 인덱싱 무시 */
	private boolean ignore;

	private boolean rawlog = true;

	public LogBean(long tableId, long scriptId, String text, String remoteAddr) {
		this.tableId = tableId;
		this.scriptId = scriptId;
		this.text = text;
		this.remoteAddr = remoteAddr;
	}

	public long getTableId() {
		return tableId;
	}

	public void setTableId(long tableId) {
		this.tableId = tableId;
	}

	public long getScriptId() {
		return scriptId;
	}

	public void setScriptId(long scriptId) {
		this.scriptId = scriptId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public ZonedDateTime getReceiveDate() {
		return receiveDate;
	}

	public void setReceiveDate(ZonedDateTime receiveDate) {
		this.receiveDate = receiveDate;
		this.timestamp = receiveDate.toInstant().toEpochMilli();
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getMessageList() {
		if (messageList.size() == 0) {
			return null;
		}

		StringBuilder stringBuilder = new StringBuilder();

		for (int index : messageList) {
			String value = (String) valueList.get(index);
			stringBuilder.append(value).append('`');
		}

		return stringBuilder.toString();
	}

	public boolean isParseComplete() {
		return parseComplete;
	}

	public void setParseComplete(boolean parseComplete) {
		this.parseComplete = parseComplete;
	}

	public void put(String key, String value) {
		put(key, value, false);
	}

	public void put(String key, String value, boolean messageListFlag) {
		/** indexing 상태일때 value가 비는경우 아무것도 안함 */
		if (indexFlag) {
			if (StringUtils.isEmpty(value)) {
				return;
			}
		}

		keyList.add(key);
		valueList.add(value);

		if (messageListFlag) {
			messageList.add(slot);
		}

		slot++;
	}

	public String getKey(int index) {
		return keyList.get(index);
	}

	public String getValue(int index) {
		return valueList.get(index);
	}

	public List<String> getKeyList() {
		return keyList;
	}

	public List<String> getValueList() {
		return valueList;
	}

	public boolean isIgnore() {
		return ignore;
	}

	public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}

	public boolean isIndexFlag() {
		return indexFlag;
	}

	public void setIndexFlag(boolean indexFlag) {
		this.indexFlag = indexFlag;
	}

	public boolean isRawlog() {
		return rawlog;
	}

	public void setRawlog(boolean rawlog) {
		this.rawlog = rawlog;
	}

	@Override
	public String toString() {
		return "LogBean [tableName=" + TableSchemaManager.getName(tableId) + ", script=" + ScriptInfoManager.getScriptName(tableId, scriptId) + ", text=" + text + ", receiveDate=" + receiveDate + "]";
	}

}
