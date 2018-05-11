package com.omnix.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    public static DateTimeFormatter defaultFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static DateTimeFormatter defaultFormat2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static DateTimeFormatter defaultFormatMin = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    public static DateTimeFormatter hourMinFormat= DateTimeFormatter.ofPattern("HH:mm");
    public static DateTimeFormatter secondMinFormat= DateTimeFormatter.ofPattern("HH:mm:ss");
    public static DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static DateTimeFormatter secFor10 = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    public static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    private static SimpleCache cache = new SimpleCache(50);
    
    private static final String rangeDelemiter = "~";
    
    public static DateFormat getDateFormat(String pattern) {
        DateFormat df = (DateFormat) cache.get(pattern);
        if (null == df) {
            df = new SimpleDateFormat(pattern);
            cache.put(pattern, df);
        }
        return df;
    }
    
    /**
     * 문자열로 된 날짜 정보를 Date 형으로 리턴한다.
     * 
     * @param text
     *            문자열로 된 날짜 정보
     * @param pattern
     *            날짜 패턴
     * @param defaultDate
     *            ParsingException이 일어나는 경우 리턴할 Date
     */
    public synchronized static Date getDate(String text, String pattern, Date defaultDate) {
        DateFormat df = getDateFormat(pattern);
        Date result = null;
        try {
            result = df.parse(text);
        } catch (Exception ie) {
            result = defaultDate;
        }
        return result;
    }
    
    /**
     * 문자열로 된 날짜 정보를 Date 형으로 리턴한다.
     * 
     * @param text
     *            문자열로 된 날짜 정보
     * @param pattern
     *            날짜 패턴
     * @return ParsingException이 일어나는 경우 null
     */
    public static Date getDate(String text, String pattern) {
        return getDate(text, pattern, (Date) null);
    }
    
    /**
     * 문자열로 된 날짜 정보를 Date 형으로 리턴한다. 패턴은 yyyy-MM-dd 이다.
     * 
     * @param text
     *            문자열로 된 날짜 정보
     * @param defaultDate
     *            ParsingException이 일어나는 경우 리턴할 Date
     */
    public static Date getDate(String text, Date defaultDate) {
        return getDate(text, "yyyy-MM-dd", defaultDate);
    }
    
    /**
     * 문자열로 된 날짜 정보를 Date 형으로 리턴한다. 패턴은 yyyy-MM-dd 이다.
     * 
     * @param text
     *            문자열로 된 날짜 정보
     * @return ParsingException이 일어나는 경우 null
     */
    public static Date getDate(String text) {
        if (10 >= text.length()) {
            return getDate(text, "yyyy-MM-dd");
        } else {
            return getDate(text, "yyyy-MM-dd HH:mm:ss");
        }
    }
    
    /**
     * 날짜 정보를 Date 형으로 리턴한다. 패턴은 yyyy-MM-dd 이다.
     * 
     * @param defaultDate
     *            ParsingException이 일어나는 경우 리턴할 Date
     */
    public static Date getDate(int year, int month, int day, Date defaultDate) {
        return getDate(year + "-" + month + "-" + day, defaultDate);
    }
    
    /**
     * 날짜 정보를 Date 형으로 리턴한다. 패턴은 yyyy-MM-dd 이다.
     * 
     * @param defaultDate
     *            ParsingException이 일어나는 경우 리턴할 Date
     */
    public static Date getDate(String year, String month, String day, Date defaultDate) {
        return getDate(year + "-" + month + "-" + day, defaultDate);
    }
    
    /**
     * 날짜 정보를 Date 형으로 리턴한다. 패턴은 yyyy-MM-dd 이다.
     * 
     * @return ParsingException이 일어나는 경우 null
     */
    public static Date getDate(int year, int month, int day) {
        return getDate(year + "-" + month + "-" + day);
    }
    
    /**
     * 날짜 정보를 Date 형으로 리턴한다. 패턴은 yyyy-MM-dd 이다.
     * 
     * @return ParsingException이 일어나는 경우 null
     */
    public static Date getDate(String year, String month, String day) {
        return getDate(year + "-" + month + "-" + day);
    }
    
    /**
     * yyyy-MM-ddTHH:mm:ss.SSS 형식을 읽어들인다.
     */
    public static Date parseDateTime(String str) {
        DateFormat df = getDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        try {
            return df.parse(str);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * yyyy-MM-ddTHH:mm:ss.SSS 형식의 문자열을 얻는다.
     */
    public static String dateTimeToString(Date date) {
        DateFormat df = getDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return df.format(date);
    }
    
    public static String dateToString(Date date) {
        DateFormat df = getDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(date);
    }
    
    public static String format(String pattern, Date date) {
        DateFormat df = getDateFormat(pattern);
        return df.format(date);
    }
    
    public static Date getStartOfDate(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    public static Date getEndOfDate(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
    
    public static Date getCurrentDay() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        // cal.set(Calendar.HOUR_OF_DAY, 0);
        // cal.set(Calendar.MINUTE, 0);
        // cal.set(Calendar.SECOND, 0);
        // cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    /**
     * 해당 날짜가 속해 있는 주의 첫째 일과 마지막 일을 구함
     * 
     * @param date
     * @return
     */
    public static Map<String, Date> getWeekDate(Date date) {
        Map<String, Date> map = new HashMap<String, Date>();
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, Calendar.SUNDAY);
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        Date firstDateOfWeek = DateUtils.addDays(date, -1 * (dayOfWeek - 1));
        Date lastDateOfWeek = DateUtils.addDays(date, 7 - dayOfWeek + 1);
        
        map.put("firstDate", firstDateOfWeek);
        map.put("lastDate", lastDateOfWeek);
        
        return map;
    }
    
    /**
     * 현재 날짜로 이전 parameter에 날짜를 구해준다.
     * 
     * @param prevDay
     * @return
     */
    public static Date getPreDate(int preDate) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -preDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    public static Date getDate(int hour, int day) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.set(Calendar.DATE, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    public static Date getDate(int hour) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    public static Date getDateWithMinute(int hour, int minute) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    public static Date getEndOfTime(int hour) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 99);
        return cal.getTime();
    }
    
    /**
     * 현재날짜기준 이전 (다음)월을 구한다.
     * 
     * @param date
     * @param month
     * @return
     */
    public static Date getMonthDate(Date date, int month) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(Calendar.MONTH, month);
        cal.add(Calendar.DATE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    /**
     * 현재날짜기준 이전 (다음)주을 구한다.
     * 
     * @param date
     * @param month
     * @return
     */
    public static Date getWeekDate(Date date, int week) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(Calendar.MONTH, 0);
        cal.add(Calendar.DATE, 7 * week);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    /**
     * 날짜로 이전 parameter에 날짜를 구해준다.
     * 
     * @param prevDay
     * @return
     */
    public static Date getPreDate(Date date, int preDate) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(Calendar.DATE, -preDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    /**
     * 현재 날짜로 이후 parameter에 날짜를 구해준다.
     * 
     * @param prevDay
     * @return
     */
    public static Date getNextDate(int nextDate) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, nextDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    /**
     * 날짜로 이후 parameter에 날짜를 구해준다.
     * 
     * @param prevDay
     * @return
     */
    public static Date getNextDate(Date date, int nextDate) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(Calendar.DATE, nextDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    /**
     * 월초에 값을 가져온다 ex) '2013-05-01 00:00:00'
     */
    public static Date getMonthFirstDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        
        Calendar calendar2 = Calendar.getInstance();
        calendar2.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1);
        return getStartOfDate(calendar2.getTime());
    }
    
    /**
     * 월말에 값을 가져온다 ex) '2013-05-31 23:59:59.999'
     */
    public static Date getMonthLastDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        
        Calendar calendar2 = Calendar.getInstance();
        calendar2.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, 0);
        return getEndOfDate(calendar2.getTime());
    }
    
    /**
     * 두 날짜의 일수 차이를 구한다. endDay - startDay
     */
    public static long getDiffDay(Date startDay, Date endDay) {
        long endDayTime = endDay.getTime();
        long startDayTime = startDay.getTime();
        long result = (endDayTime - startDayTime) / 86400000;
        return result;
    }
    
    /**
     * 유닉스시간을 스트링으로 변환 yyyy-MM-dd HH:mm:ss ex) 1399459378 (초단위).
     */
    public static String getUnixToString(long unixTime) {
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String result = format2.format(unixTime * 1000);
        return result;
    }
    
    /**
     * 날짜 String -> Date 변환
     * <p>
     * 시/분/초가 포함된 String 날짜 형식을 Date로 변환후 반환한다. 시/분/초가 포함되지 않은 날짜 형식은 {@link #getDate(String, String, Date)}을 사용
     * 
     * @param text
     * @param pattern
     * @param defaultDate
     * @return
     */
    public static Date getDateTime(String text, String pattern, Date defaultDate) {
        
        if (StringUtils.isEmpty(text) || StringUtils.isEmpty(pattern)) {
            return defaultDate;
        }
        
        // Pattern를 초과하는 날짜 String인 경우 길이를 맞춘후 변환.
        if (text.length() != pattern.length()) {
            if (text.length() > pattern.length()) {
                text = text.substring(0, pattern.length());
            } else {
                pattern = pattern.substring(0, text.length());
            }
        }
        
        Date result = null;
        try {
            LocalDateTime dateTime = LocalDateTime.parse(text, timeFormat);
            Instant instant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
            result = Date.from(instant);
        } catch (Exception e) {
            result = defaultDate;
        }
        return result;
    }
    
    /**
     * 유닉스시간을 Date타입으로 변환 yyyy-MM-dd HH:mm:ss ex) 1399459378 (초단위).
     */
    public static Date getUnixToDate(long unixTime) {
        Date result = getDate(getUnixToString(unixTime), "yyyy-MM-dd HH:mm:ss");
        return result;
    }
    
    public static LocalDateTime parseDate(String date, String format) {
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(format));
    }
    
    public static LocalDateTime parseDate(String date, DateTimeFormatter formatter) {
        return LocalDateTime.parse(date, formatter);
    }
    
    public static String parseDate(LocalDateTime date) {
        return date.format(defaultFormat);
    }
    
    public static String parseDateWithoutNano(ZonedDateTime date) {
    	return date.format(defaultFormat2);
    }
    
    public static String parseDateMin(LocalDateTime date) {
        return date.format(defaultFormatMin);
    }
    
    public static String parseHourMin(ZonedDateTime date) {
    	return date.format(hourMinFormat);
    }
    
    public static String parseDay(LocalDateTime date) {
        return date.format(dayFormat);
    }
    
    public static String parseDay(ZonedDateTime date) {
        return date.format(dayFormat);
    }
    
    public static String parseDate(LocalDateTime date, DateTimeFormatter formatter) {
        return date.format(formatter);
    }
    
    public static String parseDate(LocalDateTime date, String format) {
        return date.format(DateTimeFormatter.ofPattern(format));
    }
    
    public static String parseDateString(String date, String inFormat, String outFormat) {
        LocalDateTime time = parseDate(date, inFormat);
        return parseDate(time, outFormat);
    }
    
    public static String parseDateFor10Sec(LocalDateTime date) {
        String dateTime = date.format(secFor10);
        return StringUtils.substring(dateTime, 0, -1);
    }
    
    public static long makeTimeStamp(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
    
    public static LocalDateTime makeLocalDateTime(long timeStamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timeStamp), ZoneId.systemDefault());
    }
    
    public static ZonedDateTime makeZonedDateTime(long timeStamp) {
        return Instant.ofEpochMilli(timeStamp).atZone(ZoneId.systemDefault());
    }
    
    public static long minusLocalDateTimeByMinute(LocalDateTime localDateTime, int minute) {
        return localDateTime.minusMinutes(minute).atZone(ZoneId.systemDefault()).toEpochSecond();
    }
    
    public static String convertBoardDate(Date date) {
    	if (null == date) {
    		return "";
    	}
    	
    	ZonedDateTime dayStart = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
    	ZonedDateTime current = date.toInstant().atZone(ZoneId.systemDefault());
    	
    	if (current.isBefore(dayStart)) {
    		return current.format(dayFormat);
    	} else {
    		return current.format(secondMinFormat);
    	}
    }
    
    public static String convertHumanTime(long seconds) {
        String result = "00:00:00";
        try {
            long hours = TimeUnit.SECONDS.toHours(seconds) - TimeUnit.DAYS.toHours(TimeUnit.SECONDS.toDays(seconds));
            long minute = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds));
            long second = TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds));
            long days = TimeUnit.SECONDS.toDays(seconds);
            
            if (days > 0) {
                result = String.valueOf(days) + "day " + StringUtils.leftPad(String.valueOf(hours), 2, "0") + ":" + StringUtils.leftPad(String.valueOf(minute), 2, "0") + ":" + StringUtils.leftPad(String.valueOf(second), 2, "0");
            } else {
                result = StringUtils.leftPad(String.valueOf(hours), 2, "0") + ":" + StringUtils.leftPad(String.valueOf(minute), 2, "0") + ":" + StringUtils.leftPad(String.valueOf(second), 2, "0");
            }
        } catch (Exception e) {
        }
        return result;
    }
    
    
    /**
     * DD일 HH시 MM분 형식으로 반환
     * 
     * @param seconds
     * @param dayTxt
     * @param hourTxt
     * @param minTxt
     * @return
     */
    public static String convertHumanTime(long seconds, String dayTxt, String hourTxt, String minTxt) {
        String result = "00" + dayTxt + " 00" + minTxt;
        try {
            long hours = TimeUnit.SECONDS.toHours(seconds) - TimeUnit.DAYS.toHours(TimeUnit.SECONDS.toDays(seconds));
            long minute = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds));
            long days = TimeUnit.SECONDS.toDays(seconds);
            
            if (days > 0) {
                result = String.valueOf(days) + dayTxt + " " + StringUtils.leftPad(String.valueOf(hours), 2, "0") + hourTxt + " " + StringUtils.leftPad(String.valueOf(minute), 2, "0") + minTxt;
            } else {
                result = StringUtils.leftPad(String.valueOf(hours), 2, "0") + hourTxt + " " + StringUtils.leftPad(String.valueOf(minute), 2, "0") + minTxt;
            }
        } catch (Exception e) {
        }
        return result;
    }
    
	
	public static Map<Long, String> getDaysDateRange(ZonedDateTime startDate, ZonedDateTime endDate, int intervalMin, int minusHourKey) {
		Map<Long, String> map = new HashMap<>();
		
		while (true) {
			StringBuilder dateQuery = new StringBuilder();

				dateQuery.append(DateUtils.parseDateWithoutNano(startDate)).append(rangeDelemiter).append(DateUtils.parseDateWithoutNano(startDate.plusMinutes(intervalMin)));
				map.put(startDate.minusHours(minusHourKey).toInstant().toEpochMilli(), dateQuery.toString());
				startDate = startDate.plusMinutes(intervalMin);

			if (startDate.isAfter(endDate) || startDate.isEqual(endDate)) {
				break;
			}
		}
		return map;
	}
	
}
