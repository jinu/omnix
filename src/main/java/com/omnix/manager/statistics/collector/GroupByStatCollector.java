package com.omnix.manager.statistics.collector;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.DocumentMapper;
import com.omnix.manager.parser.LogFieldType;
import com.omnix.manager.statistics.DocumentStatUtils;
import com.omnix.manager.statistics.DocumentsStat;
import com.omnix.manager.statistics.GroupBySort;

public class GroupByStatCollector extends AggregateCollector {
    /** group by 가 동작할 필드명 */
    private final String keyField;
    private List<ColumnInfo> columnInfos;
    
    /** column 만큼의 해당 총합을 구한다.  */
    private double[] totalValues;
    
    private long count = 0;
    
    private long limit;
    private boolean full;
    private boolean countOnly;
    
    private final GroupBySort groupBySort;
    
    /** SortedDocValues */
    private SortedDocValues keyIndex;
    private List<NumericDocValues> numericIndexes = new ArrayList<>();
    
    /** slot */
    private List<DocumentsStat[]> documentsStatsSlots = new ArrayList<>();
    
    /** slot for count */
    private int[] countSlot;
    
    /** result */
    private LoadingCache<String, List<DocumentsStat>> resultMap;
    
    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public GroupByStatCollector(List<ColumnInfo> columnInfos, long limit, GroupBySort groupBySort) {
    	ColumnInfo groupColumn = columnInfos.get(0);
    	String keyName = groupColumn.getName();
    	
    	/** numeric 일 경우, '_'를 붙여 sorted 값을 얻어올 수 있도록 한다. */
    	if (groupColumn.getLogFieldType().isInteger()) {
    		keyName = DocumentMapper.STORE_DELEMETER + groupColumn.getName();
		}
    	
        this.keyField = keyName; // 0번은 항상 keyField
        this.limit = limit;
        
        /** columnInfo 가 null 인 경우 count만 collect한다. */
        if (columnInfos.size() == 1 || columnInfos.get(1) == null) {
            this.countOnly = true;
        } else {
            List<ColumnInfo> list = new ArrayList<>();
            for (int i = 1; i < columnInfos.size(); i++) {
                list.add(columnInfos.get(i));
            }
            
            this.columnInfos = list;
            totalValues = new double[list.size()];
        }
        
        this.groupBySort = groupBySort;
        
        resultMap = CacheBuilder.newBuilder().maximumSize(DEFAULT_SLOT_COUNT).build(CacheLoader.from(key -> {
        	DocumentsStat documentsStat = new DocumentsStat();
            documentsStat.setKey(key);
            
            List<DocumentsStat> lists = new ArrayList<>();
            lists.add(documentsStat);
            		
            return lists;
        }));
    }
    
    @Override
    protected void doSetNextReader(LeafReaderContext context) throws IOException {
        merge();
        
        try {
            /** index 가 변경될때 처리함. */
            keyIndex = DocValues.getSorted(context.reader(), keyField);
        } catch (IllegalStateException e) {
            /** statistics indexing 변경이 일어난 경우, 해당 context는 건너 뛴다. */
            keyIndex = null;
            return;
        }
        
        /** unique 한 key들의 count 값을 가져온다. */
        int keyCount = keyIndex.getValueCount();
        
        if (countOnly) {
            countSlot = new int[keyCount];
        } else {
            for (int i = 0; i < columnInfos.size(); i++) {
                ColumnInfo columnInfo = columnInfos.get(i);
                
                NumericDocValues numericIndex = null;
                DocumentsStat[] documentsStatsSlot = null;
                try {
                    numericIndex = DocValues.getNumeric(context.reader(), columnInfo.getName());
                    documentsStatsSlot = new DocumentsStat[keyCount];
                    
                    for (int j = 0; j < keyCount; j++) {
                        documentsStatsSlot[j] = new DocumentsStat(j);
                    }
                } catch (IllegalStateException e) {
                    logger.info("reader error.");
                }
                
                numericIndexes.add(numericIndex);
                documentsStatsSlots.add(documentsStatsSlot);
            }
        }
        
        logger.debug("slotSize : {}", keyCount);
    }
    
    @Override
    public boolean needsScores() {
        return false;
    }
    
    @Override
    public void collect(int doc) throws IOException {
        if (full || (limit > 0 && count >= limit)) {
            this.full = true;
            return;
        }
        
        ++count;
        if (countOnly) {
            doAccumulateOnlyCount(doc);
        } else {
            for (int i = 0; i < columnInfos.size(); i++) {
                doAccumulate(doc, columnInfos.get(i), numericIndexes.get(i), documentsStatsSlots.get(i), i);
            }
            
        }
    }
    
    public void doAccumulateOnlyCount(int doc) throws IOException {
        if (keyIndex == null) {
            return;
        }
        
        boolean flag = keyIndex.advanceExact(doc);
        if (!flag) {
            return;
        }
        
        int ord = keyIndex.ordValue();
        countSlot[ord]++;
    }
    
    public void doAccumulate(int doc, ColumnInfo columnInfo, NumericDocValues numericIndex, DocumentsStat[] documentsStatsSlot, int index) throws IOException {
        if (keyIndex == null || numericIndexes.size() == 0) {
            return;
        }
        
        boolean keyFlag = keyIndex.advanceExact(doc);
        if (!keyFlag) {
            return;
        }
        
        boolean numericFlag = numericIndex.advanceExact(doc);
        double val = 0L;
        if (numericFlag) {
            if (columnInfo.getLogFieldType() == LogFieldType.DOUBLE) {
                val = Double.longBitsToDouble(numericIndex.longValue());
            } else {
            	val = numericIndex.longValue();
            }
        }
        totalValues[index] += val;
        
        int ord = keyIndex.ordValue();
        DocumentsStat documentsStat = documentsStatsSlot[ord];
        
        documentsStat.addCount(1L);
        documentsStat.addSum(val);
        documentsStat.addPow2sum(Math.pow(val, 2));
        documentsStat.setFlag(true);
        
        if (val > documentsStat.getMax()) {
            documentsStat.setMax(val);
        }
        
        if (val < documentsStat.getMin()) {
            documentsStat.setMin(val);
        }
    }
    
    @Override
    public void merge() throws IOException {
        if (countOnly) {
            if (null == countSlot) {
                return;
            }
            
            for (int i = 0; i < countSlot.length; i++) {
                if (countSlot[i] > 0) {
                    String key = keyIndex.lookupOrd(i).utf8ToString();
                    List<DocumentsStat> lists;
					try {
						lists = resultMap.get(key);
						lists.get(0).addCount(countSlot[i]);
					} catch (ExecutionException e) {
						logger.error("merge error", e);
					}
                    
                }
            }
        } else {
            if (CollectionUtils.isEmpty(documentsStatsSlots)) {
                return;
            }
            
            for (int i = 0; i < columnInfos.size(); i++) {
                for (DocumentsStat documentsStat : documentsStatsSlots.get(i)) {
                    if (documentsStat.isFlag()) {
                        int slot = documentsStat.getSlot();
                        String key = keyIndex.lookupOrd(slot).utf8ToString();
                        
						try {
							List<DocumentsStat> oldDocumentStats = resultMap.get(key);
							
							/** slot 에 맞게 merge 처리 */
	                        if (oldDocumentStats.size() == i) {
	                            documentsStat.setKey(key);
	                            
	                            documentsStat.makeAvg();
	                            documentsStat.makeStdev();
	                            
	                            oldDocumentStats.add(documentsStat);
	                            
	                        } else {
	                            DocumentsStat oldDocumentStat = oldDocumentStats.get(i);
	                            
	                            oldDocumentStat.addCount(documentsStat.getCount());
	                            oldDocumentStat.addSum(documentsStat.getSum());
	                            oldDocumentStat.addPow2sum(documentsStat.getPow2sum());
	                            oldDocumentStat.makeAvg();
	                            oldDocumentStat.makeStdev();
	                            
	                            if(documentsStat.getMax() > oldDocumentStat.getMax()) {
	                                oldDocumentStat.setMax(documentsStat.getMax());
	                            }
	                            
	                            if(documentsStat.getMin() < oldDocumentStat.getMin()) {
	                                oldDocumentStat.setMin(documentsStat.getMin());
	                            }
	                        }
						} catch (ExecutionException e) {
							logger.error("merge error", e);
						}
                    }
                }
            }
            
        }
        
        numericIndexes.clear();
        documentsStatsSlots.clear();
        
        countSlot = null;
        keyIndex = null;
    }
    
    @Override
    public List<String> getValues(int count) {
        List<String> list = resultMap.asMap().entrySet().stream().sorted((entry1, entry2) -> {
            return DocumentStatUtils.compare(entry1.getValue(), entry2.getValue(), groupBySort);
        }).limit(count).map(entry -> DocumentStatUtils.makeStringFromDocuments(entry.getValue())).collect(Collectors.toList());
        return list;
    }
    
    @Override
    public long getTotalCount() {
        return count;
    }
    
    @Override
    public boolean isFull() {
        return full;
    }

	public Map<String, List<DocumentsStat>> getResultMap() {
        return resultMap.asMap();
    }

	@Override
    public void startHistogramCollect(boolean flag) {
    }

    @Override
    public void endHistogramCollect(String indexKey, LocalDateTime start, LocalDateTime finish) {
    }
    
    @Override
    public Map<String, Map<String, List<DocumentsStat>>> getHistogramMap() {
        return new HashMap<>();
    }
    
	@Override
	public List<Double> getTotalValues() {
		if (totalValues == null) {
			return new ArrayList<>();
		}
		
		return Arrays.stream(totalValues).boxed().collect(Collectors.toList());
				
	}
}
