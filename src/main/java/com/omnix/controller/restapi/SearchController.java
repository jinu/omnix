package com.omnix.controller.restapi;

import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.omnix.config.UiValidationException;
import com.omnix.manager.parser.ColumnInfoManager;
import com.omnix.manager.search.IndexSearchManager;
import com.omnix.manager.search.SearchResult;
import com.omnix.manager.search.SearchSupport;
import com.omnix.util.QueryBuilder;
import com.omnix.util.SearchUtils;

@Controller
@RequestMapping("/restapi/search")
public class SearchController {
	@Autowired
	private IndexSearchManager indexSearchManager;

	@RequestMapping(value = { "/{tableId}/queryCheck", })
	@ResponseBody
	public boolean queryCheck(@PathVariable("tableId") long tableId, SearchSupport searchSupport) {
		try {
			QueryBuilder builder = new QueryBuilder(searchSupport.getQuery(), ColumnInfoManager.getColumnInfoCache(tableId));
			builder.build();

		} catch (ParseException e) {

			String[] args = { e.getMessage() };
			throw new UiValidationException("ALERT_SEARCH_FAIL", args);
		}

		return true;
	}

	@RequestMapping(value = { "/{tableId}/count", })
	@ResponseBody
	public SearchResult searchCount(@PathVariable("tableId") long tableId, SearchSupport searchSupport) {
		return indexSearchManager.searchCount(searchSupport, tableId, false);
	}

	@RequestMapping(value = { "/{tableId}" })
	@ResponseBody
	public SearchResult search(@PathVariable("tableId") long tableId, SearchSupport searchSupport) {
		SearchResult result = indexSearchManager.search(searchSupport, tableId, false);
		SearchUtils.make1HourHistogram(result, 60);
		return result;
	}

	@RequestMapping(value = { "/{tableId}/cancel" })
	@ResponseBody
	public boolean cancel(@PathVariable("tableId") long tableId, SearchSupport searchSupport) throws Exception {
		return indexSearchManager.cancel(searchSupport);
	}

	@RequestMapping(value = { "/{tableId}/delete" })
	@ResponseBody
	public SearchResult searchCountDelete(@PathVariable("tableId") long tableId, SearchSupport searchSupport) {
		return indexSearchManager.searchCount(searchSupport, tableId, true);
	}

}
