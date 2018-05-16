package com.omnix.controller.restapi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.omnix.config.UiValidationException;
import com.omnix.manager.search.IndexSearchManager;
import com.omnix.manager.search.SearchResult;
import com.omnix.manager.search.SearchSupport;
import com.omnix.manager.statistics.AggregateResult;
import com.omnix.manager.statistics.AggregateSupport;
import com.omnix.manager.statistics.DocumentStatUtils;
import com.omnix.manager.statistics.DocumentsStat;
import com.omnix.manager.statistics.IndexStatisticsManager;
import com.omnix.util.SearchUtils;

@Controller
@RequestMapping("/restapi/aggregate")
public class StatisticsController {
	@Autowired
	private IndexStatisticsManager indexStatisticsManager;
	@Autowired
	private IndexSearchManager indexSearchManager;

	/** Logger */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@RequestMapping(value = { "/{tableId}" })
	@ResponseBody
	public AggregateResult statistics(@PathVariable("tableId") long tableId, AggregateSupport aggregateSupport) {
		try {
			AggregateResult aggregateResult = null;
			List<LocalDateTime> lists = SearchUtils.getBetweenDate(aggregateSupport.getSearchType(), aggregateSupport.getDateRange());
			List<String> targets = SearchUtils.searchTargetList(lists.get(0), lists.get(1), aggregateSupport.isReverse());

			/** count 쿼리로 대체 처리 */
			if (ArrayUtils.isEmpty(aggregateSupport.getColumns()) && targets.size() >= 3) {
				SearchSupport searchSupport = new SearchSupport();
				searchSupport.setDateRange(aggregateSupport.getDateRange());
				searchSupport.setLimit(aggregateSupport.getLimit());
				searchSupport.setQuery(aggregateSupport.getQuery());
				searchSupport.setSearchType(aggregateSupport.getSearchType());

				SearchResult result = indexSearchManager.searchCount(searchSupport, tableId, false);

				DocumentsStat documentsStatTotal = new DocumentsStat();
				documentsStatTotal.setCount(result.getCount());
				documentsStatTotal.setMin(0);
				documentsStatTotal.setMax(0);

				aggregateResult = new AggregateResult();
				aggregateResult.setDate(result.getDate());
				aggregateResult.setQueryTime(result.getQueryTime());
				aggregateResult.setCount(result.getCount());
				aggregateResult.setResult(Arrays.asList(DocumentStatUtils.makeStringFromDocument(documentsStatTotal)));

				Map<String, Map<String, List<DocumentsStat>>> histogramResult = new LinkedHashMap<>();

				result.getHistogram().entrySet().forEach(entry -> {
					String dateKey = entry.getKey();
					int count = entry.getValue();

					Map<String, List<DocumentsStat>> subMap = new LinkedHashMap<>();
					List<DocumentsStat> documentsStatList = new ArrayList<>();

					DocumentsStat documentsStat = new DocumentsStat();
					documentsStat.setCount(count);
					documentsStat.setMin(0);
					documentsStat.setMax(0);

					documentsStatList.add(documentsStat);
					subMap.put("", documentsStatList);
					histogramResult.put(dateKey, subMap);
				});

				aggregateResult.setHistogramResult(histogramResult);

			} else {
				aggregateResult = indexStatisticsManager.statistics(aggregateSupport, tableId);
			}

			return aggregateResult;

		} catch (Exception e) {
			logger.error("statistics error", e);
			throw new UiValidationException("ALERT_SEARCH_FAIL");
		}

	}

	@RequestMapping(value = { "/{tableId}/cancel" })
	@ResponseBody
	public boolean cancel(@PathVariable("tableId") long tableId, AggregateSupport aggregateSupport) throws Exception {
		return indexStatisticsManager.cancel(aggregateSupport);
	}

}
