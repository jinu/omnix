package com.omnix.manager.statistics.collector;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.SimpleCollector;

import com.omnix.manager.statistics.DocumentsStat;

public abstract class AggregateCollector extends SimpleCollector {
    /** merge */
    public abstract void merge() throws IOException;
    
    /** DocumentsStat 결과 return */
    public abstract List<String> getValues(int count);
    
    /** limit에 걸리는 경우 true를 반환한다. */
    public abstract boolean isFull();
    
    /** 전체 count */
    public abstract long getTotalCount();
    
    /** 전체 count */
    public abstract List<Double> getTotalValues();
    
    /** sampling count */
    public static final int DEFAULT_SLOT_COUNT = 300_0000;
    
    /**  histogram 사용 여부 */
    protected boolean histogramCollectFlag = false;
    
    /** histogram이 필요 한 경우 */
    public abstract void startHistogramCollect(boolean flag);
    public abstract void endHistogramCollect(String indexKey, LocalDateTime start, LocalDateTime finish);
    public abstract Map<String, Map<String, List<DocumentsStat>>> getHistogramMap();
}