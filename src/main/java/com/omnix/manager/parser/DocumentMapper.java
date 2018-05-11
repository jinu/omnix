package com.omnix.manager.parser;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.InetAddressPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseReader;

@Component
public class DocumentMapper {
	public static DatabaseReader databaseReader;
    /** Logger */
    public static final Logger logger = LoggerFactory.getLogger(DocumentMapper.class);
    public static final ConcurrentHashMap<String, String> countryCache = new ConcurrentHashMap<>();
    
    public static final String STORE_DELEMETER = "_";
    
    @PostConstruct
    public void init() {
        try (InputStream is = ResourceUtils.getURL("classpath:geo/GeoLite2-Country.mmdb").openStream()) {
            databaseReader = new DatabaseReader.Builder(is).withCache(new CHMCache(8192)).fileMode(Reader.FileMode.MEMORY).build();
        } catch (Exception e) {
            logger.error("failed to init GeoIpManager. ", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
    }
    
    /**
     * 검색 필드를 정의한다.
     * 
     * @throws UnknownHostException
     */
    public List<Field> makeFieldBySearch(ColumnInfo columnInfo, String text) throws UnknownHostException {
        List<Field> fields = new ArrayList<>();
        
        if (null == columnInfo.getLogFieldType() || !columnInfo.isSearch()) {
            return null;
        }
        
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        
        String name = columnInfo.getName();
        switch (columnInfo.getLogFieldType()) {
        case STRING:
            fields.add(new StringField(name, StringUtils.lowerCase(text), Store.NO));
            break;
        
        case INT:
            fields.add(new IntPoint(name, NumberUtils.toInt(text)));
            break;
        
        case LONG:
        case DATETIME:
            fields.add(new LongPoint(name, NumberUtils.toLong(text)));
            break;
        
        case DOUBLE:
            fields.add(new DoublePoint(name, NumberUtils.toDouble(text)));
            break;
        
        case FLOAT:
            fields.add(new FloatPoint(name, NumberUtils.toFloat(text)));
            break;
        
        case IP:
            InetAddress inetAddress = InetAddress.getByName(text);
            fields.add(new InetAddressPoint(name, inetAddress));
            fields.add(new StringField(name, text, Store.NO));
            break;
        
        case COUNTRY:
            String countryCode = StringUtils.lowerCase(getCountryCode(text));
            fields.add(new StringField(name, countryCode, Store.NO));
            break;
        
        case TEXT:
            fields.add(new TextField(name, StringUtils.lowerCase(text), Store.NO));
            break;
        
        default:
            break;
        }
        
        return fields;
    }
    
    /** 통계 필드를 정의한다. */
    public List<Field> makeFieldByStatistics(ColumnInfo columnInfo, String text) {
    	if (null == columnInfo.getLogFieldType() || !columnInfo.isStatistics()) {
            return null;
        }
    	
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        
        List<Field> fields = new ArrayList<>();
        
        String name = columnInfo.getName();
        switch (columnInfo.getLogFieldType()) {
        case STRING:
        case TEXT:
        case IP:
        	fields.add(new SortedDocValuesField(name, new BytesRef(text)));
            break;
        
        case COUNTRY:
        	fields.add(new SortedDocValuesField(name, new BytesRef(getCountryCode(text))));
            break;
        
        case INT:
        	StringBuilder keyBuilder = new StringBuilder();
        	keyBuilder.append(STORE_DELEMETER).append(name);
        	fields.add(new SortedDocValuesField(keyBuilder.toString(), new BytesRef(text)));
        case LONG:
        case DATETIME:
        	fields.add(new NumericDocValuesField(name, NumberUtils.toLong(text, 0L)));
            break;
        
        case DOUBLE:
        	fields.add(new DoubleDocValuesField(name, NumberUtils.toDouble(text, 0)));
            break;
        
        case FLOAT:
        	fields.add(new FloatDocValuesField(name, NumberUtils.toFloat(text, 0)));
            break;
        
        default:
            break;
        }
        return fields;
    }
    
    /**
     * 국가코드를 조회한다.
     */
    public static String getCountryCode(String ip) {
    	/** ip 길이 체크 */
    	if(ip.length() <= 6) {
    		return "";
    	}
    	
        /** cache에 존재 할 경우 cache에서 꺼낸다. */
        String country = countryCache.get(ip);
        
        if (country != null) {
            return country;
        }
        
        try {
            country = StringUtils.defaultString(databaseReader.country(InetAddress.getByName(ip)).getCountry().getIsoCode(), "");
            countryCache.put(ip, country);
        } catch (Exception e) {
            logger.debug("getCountryCode error", e);
            return "";
        }
        return country;
    }
}
