package com.omnix.manager.search;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * 검색 기준
 */
public enum SearchType {
    CUSTOM, HOUR, DAY, YESTERDAY, DAYS_3, WEEK, WEEK2, MONTH;
    
    public LocalDateTime getDay(LocalDateTime now) {
        LocalDateTime result = null;
        
        switch (this) {
        case HOUR:
            result = now.withNano(0).minusHours(1L);
            break;
            
        case DAY:
            result = now.withNano(0).withSecond(0).withMinute(0).withHour(0);
            break;
        
        case YESTERDAY:
            result = now.withNano(0).withSecond(0).withMinute(0).withHour(0).minusDays(1L);
            break;
            
        case DAYS_3:
            result = now.withNano(0).withSecond(0).withMinute(0).withHour(0).minusDays(3L);
            break;
        
        case WEEK:
            result = now.withNano(0).withSecond(0).withMinute(0).withHour(0).with(DayOfWeek.MONDAY);
            break;
            
        case WEEK2:
            result = now.withNano(0).withSecond(0).withMinute(0).withHour(0).with(DayOfWeek.MONDAY).minusWeeks(1L);
            break;
        
        case MONTH:
            result = now.withNano(0).withSecond(0).withMinute(0).withHour(0).withDayOfMonth(1);
            break;
            
        default:
            break;
        }
        
        return result;
    }
    
    public LocalDateTime getDayEnd(LocalDateTime now) {
        LocalDateTime result = now.withNano(0).withSecond(0).withMinute(0).withHour(0).plusDays(1L).minusNanos(1000L);
        return result;
    }
    
    public String getDayString() {
        String result = "";
        
        switch (this) {
        case HOUR:
            result = "최근1시간";
            break;
            
        case DAY:
            result = "오늘";
            break;
        
        case YESTERDAY:
            result = "어제부터";
            break;
            
        case DAYS_3:
            result = "3일전부터";
            break;
        
        case WEEK:
            result = "이번주부터";
            break;
            
        case WEEK2:
            result = "저번주부터";
            break;
        
        case MONTH:
            result = "이번달부터";
            break;
            
        case CUSTOM:
        default:
            result = "사용자정의";
            break;
        }
        
        return result;
    }
}

