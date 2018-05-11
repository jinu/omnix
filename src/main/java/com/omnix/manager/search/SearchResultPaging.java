package com.omnix.manager.search;

public class SearchResultPaging {
    private String key;
    private int offset;
    private int limit;
    
    public SearchResultPaging(String key, int offset, int limit) {
        this.key = key;
        this.offset = offset;
        this.limit = limit;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    @Override
    public String toString() {
        return "SearchResultPaging [key=" + key + ", offset=" + offset + ", limit=" + limit + "]";
    }
    
}
