package com.omnix.manager.statistics.collector;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.omnix.manager.parser.ColumnInfo;
import com.omnix.manager.parser.DocumentMapper;
import com.omnix.manager.parser.LogFieldType;
import com.omnix.manager.statistics.DocumentsStat;
import com.omnix.manager.statistics.GroupBySort;

public class GroupByNCollector extends AggregateCollector {
	/** group by 가 동작할 필드명 */
	private final String[] keyFields;
	/** SortedDocValues */
	private final SortedDocValues[] keyIndexes;
	/** numericIndex */
	private NumericDocValues numericIndex;
	/** N 개수 */
	private int keySize;
	/** ord 번호에 string 넣어준 곳 */
	private final List<BytesRef[]> ordSlots = new ArrayList<>();

	private final Comparator<Entry<BytesRef, DocumentsStat>> comparatorForBytesRef;

	/** merge 전, 값을 넣어 둠 */
	private final Map<BytesRef, DocumentsStat> resultForReader = new HashMap<>();
	/** merge 후, result 값 */
	private LoadingCache<BytesRef, DocumentsStat> result;

	/** key 연산용 */
	private final ByteBuffer keyBuffer;
	
	/** total count */
	private long totalCount;
	private long limit;
	private boolean full;
	/** numeric 값이 없는 경우 */
	private boolean countOnly;

	private final ColumnInfo columnInfo;
	/** key 구분 */
	private static final String KEY_DIVISION = "_";
	
	/** Logger */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public GroupByNCollector(List<ColumnInfo> keyList, ColumnInfo columnInfo, long maxLimit, GroupBySort groupBySort) {
		this.keyFields = keyList.stream().map(keycolumn -> {
			if (keycolumn.getLogFieldType().isInteger()) {
				return DocumentMapper.STORE_DELEMETER + keycolumn.getName();
			}
			return keycolumn.getName();
		}).toArray(String[]::new);
		
		this.limit = maxLimit;
		this.keySize = keyFields.length;
		this.columnInfo = columnInfo;
		this.comparatorForBytesRef = getComparatorForBytesRef(groupBySort);

		keyIndexes = new SortedDocValues[keySize];

		/** columnInfo 가 null 인 경우 count만 collect한다. */
		if (null == columnInfo) {
			countOnly = true;
		}
		/** buffer size 할당 */
		this.keyBuffer = ByteBuffer.allocate(keySize * 4);
		
		result = CacheBuilder.newBuilder().maximumSize(DEFAULT_SLOT_COUNT).build(CacheLoader.from(key -> {
			return new DocumentsStat();
		}));
	}

	public static Comparator<Entry<BytesRef, DocumentsStat>> getComparatorForBytesRef(GroupBySort groupBySort) {
		return (Entry<BytesRef, DocumentsStat> t1, Entry<BytesRef, DocumentsStat> t2) -> {
			int compare = 0;
			switch (groupBySort) {
			case AVG:
				compare = Double.compare(t2.getValue().getAvg(), t1.getValue().getAvg());
				break;

			case SUM:
				compare = Double.compare(t2.getValue().getSum(), t1.getValue().getSum());
				break;

			case COUNT:
			default:
				compare = Long.compare(t2.getValue().getCount(), t1.getValue().getCount());

				break;
			}
			return compare;
		};
	}

	@Override
	protected void doSetNextReader(LeafReaderContext context) throws IOException {
		merge();

		logger.debug("doSetNextReader {}", context);
		
		try{
		      /** index 가 변경될때 처리함. */
	        for (int i = 0; i < keySize; i++) {
	            keyIndexes[i] = DocValues.getSorted(context.reader(), keyFields[i]);
	            int keyCount = keyIndexes[i].getValueCount();
	            ordSlots.add(new BytesRef[keyCount]);
	        }

	        if (!countOnly) {
	            numericIndex = DocValues.getNumeric(context.reader(), columnInfo.getName());
	        }
        } catch (IllegalStateException e) {
            /** index 변경으로, 통계 index가 없을 시 건너 뛴다. */
            Arrays.stream(keyIndexes).forEach(keyindex -> keyindex = null);
            numericIndex = null;
        }
	}

	@Override
	public void collect(int doc) throws IOException {
       if(keyIndexes == null) {
            return;
        }
	    
		if (limit > 0 && totalCount >= limit) {
			this.full = true;
			return;
		}

		keyBuffer.position(0);

		for (SortedDocValues keyIndex : keyIndexes) {
			boolean flag = keyIndex.advanceExact(doc);
			if (!flag) {
				keyBuffer.putInt(-1);
				return;
			}
			int ord = keyIndex.ordValue();
			keyBuffer.putInt(ord);
		}

		byte[] temp = new byte[keySize * 4];
		keyBuffer.position(0);
		keyBuffer.get(temp);
		BytesRef key = new BytesRef(temp);

		DocumentsStat documentStat = resultForReader.get(key);
		if (null == documentStat) {
			documentStat = new DocumentsStat();
			resultForReader.put(key, documentStat);
		}

		if (!countOnly) {
			boolean numericFlag = numericIndex.advanceExact(doc);

			if (numericFlag) {
				double val = 0L;

				if (columnInfo.getLogFieldType() == LogFieldType.DOUBLE) {
					val = Double.longBitsToDouble(numericIndex.longValue());
				} else {
					val = numericIndex.longValue();
				}
				
				documentStat.addSum(val);
				documentStat.addPow2sum(Math.pow(val, 2));
				documentStat.setFlag(true);

				if (val > documentStat.getMax()) {
					documentStat.setMax(val);
				}

				if (val < documentStat.getMin()) {
					documentStat.setMin(val);
				}
			}
		}
		documentStat.addCount(1L);

		totalCount++;
	}

	@Override
	public void merge() {
		logger.debug("merge start. size : {}, totalCount : {}", resultForReader.entrySet().size(), totalCount);

		Stream<Entry<BytesRef, DocumentsStat>> stream = resultForReader.entrySet().stream();
		if (resultForReader.entrySet().size() > 100_0000) {
			stream = stream.sorted(comparatorForBytesRef).limit(100_0000);
		}
		/** key 값을 byte 값으로 연산 */
		final BytesRefBuilder keyBuilder = new BytesRefBuilder();
		
		stream.forEach(entry -> {
			keyBuffer.position(0);
			ByteBuffer ordBuffer = keyBuffer.put(entry.getKey().bytes);
			keyBuffer.position(0);
			
			for (int i = 0; i < keySize; i++) {
				int ord = ordBuffer.getInt();
				BytesRef value = ordSlots.get(i)[ord];
				
				if (null == value) {
					try {
						value = keyIndexes[i].lookupOrd(ord);
					} catch (IOException e) {
						logger.error("merge error", e);
					}
					ordSlots.get(i)[ord] = BytesRef.deepCopyOf(value);
				} 
				keyBuilder.append(value);
				keyBuilder.append(new BytesRef(KEY_DIVISION.getBytes()));
			}
			BytesRef key = keyBuilder.toBytesRef();
			keyBuilder.clear();
			
			DocumentsStat newDocumentStat = entry.getValue();
			try {
				DocumentsStat oldDocumentStat = result.get(key);
				
				oldDocumentStat.addCount(newDocumentStat.getCount());
				if (!countOnly) {
					oldDocumentStat.addSum(newDocumentStat.getSum());
					oldDocumentStat.addPow2sum(newDocumentStat.getPow2sum());
					oldDocumentStat.makeAvg();
					oldDocumentStat.makeStdev();
				}
			} catch (ExecutionException e) {
				logger.error("merge error", e);
			}
		});

		resultForReader.clear();
		ordSlots.clear();
	}

	@Override
	public boolean isFull() {
		return full;
	}

	@Override
	public long getTotalCount() {
		return totalCount;
	}

	@Override
	public List<String> getValues(int count) {
		return result.asMap().entrySet().stream().sorted(comparatorForBytesRef).limit(count).map(entry -> {
			/** key 값을 String으로 변환 */
			entry.getValue().setKey(entry.getKey().utf8ToString());
			
			return entry.getValue().toCsvString();
		}).collect(Collectors.toList());
	}

	@Override
	public boolean needsScores() {
		return false;
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
		return new ArrayList<>();
	}
}
