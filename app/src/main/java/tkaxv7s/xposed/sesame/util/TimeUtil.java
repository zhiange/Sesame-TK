package tkaxv7s.xposed.sesame.util;

import android.annotation.SuppressLint;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 时间工具类。
 * 提供了一系列方法来处理时间相关的操作，包括时间范围检查、时间比较、日期格式化等。
 */
public class TimeUtil {

    /**
     * 检查当前时间是否在给定的时间范围内。
     *
     * @param timeRange 时间范围，格式为 "HH-mm-HH-mm"。
     * @return 如果当前时间在范围内，返回 true，否则返回 false。
     */
    public static Boolean checkNowInTimeRange(String timeRange) {
        return checkInTimeRange(System.currentTimeMillis(), timeRange);
    }

    /**
     * 检查给定的时间毫秒数是否在给定的时间范围列表中。
     *
     * @param timeMillis    时间毫秒数。
     * @param timeRangeList 时间范围列表，每个范围格式为 "HH-mm-HH-mm"。
     * @return 如果时间在任一范围内，返回 true，否则返回 false。
     */
    public static Boolean checkInTimeRange(Long timeMillis, List<String> timeRangeList) {
        for (String timeRange : timeRangeList) {
            if (checkInTimeRange(timeMillis, timeRange)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查给定的时间毫秒数是否在给定的时间范围内。
     *
     * @param timeMillis 时间毫秒数。
     * @param timeRange  时间范围，格式为 "HH-mm-HH-mm"。
     * @return 如果时间在范围内，返回 true，否则返回 false。
     */
    public static Boolean checkInTimeRange(Long timeMillis, String timeRange) {
        try {
            String[] timeRangeArray = timeRange.split("-");
            if (timeRangeArray.length == 2) {
                String min = timeRangeArray[0];
                String max = timeRangeArray[1];
                return isAfterOrCompareTimeStr(timeMillis, min) && isBeforeOrCompareTimeStr(timeMillis, max);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return false;
    }

    /**
     * 检查当前时间是否在给定的时间字符串之前。
     *
     * @param beforeTimeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 如果当前时间在之前，返回 true，否则返回 false。
     */
    public static Boolean isNowBeforeTimeStr(String beforeTimeStr) {
        return isBeforeTimeStr(System.currentTimeMillis(), beforeTimeStr);
    }

    /**
     * 检查当前时间是否在给定的时间字符串之后。
     *
     * @param afterTimeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 如果当前时间在之后，返回 true，否则返回 false。
     */
    public static Boolean isNowAfterTimeStr(String afterTimeStr) {
        return isAfterTimeStr(System.currentTimeMillis(), afterTimeStr);
    }

    /**
     * 检查当前时间是否在给定的时间字符串之前或相等。
     *
     * @param beforeTimeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 如果当前时间在之前或相等，返回 true，否则返回 false。
     */
    public static Boolean isNowBeforeOrCompareTimeStr(String beforeTimeStr) {
        return isBeforeOrCompareTimeStr(System.currentTimeMillis(), beforeTimeStr);
    }

    /**
     * 检查当前时间是否在给定的时间字符串之后或相等。
     *
     * @param afterTimeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 如果当前时间在之后或相等，返回 true，否则返回 false。
     */
    public static Boolean isNowAfterOrCompareTimeStr(String afterTimeStr) {
        return isAfterOrCompareTimeStr(System.currentTimeMillis(), afterTimeStr);
    }

    /**
     * 检查给定的时间毫秒数是否在给定的时间字符串之前。
     *
     * @param timeMillis    时间毫秒数。
     * @param beforeTimeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 如果时间在之前，返回 true，否则返回 false。
     */
    public static Boolean isBeforeTimeStr(Long timeMillis, String beforeTimeStr) {
        Integer compared = isCompareTimeStr(timeMillis, beforeTimeStr);
        if (compared != null) {
            return compared < 0;
        }
        return false;
    }

    /**
     * 检查给定的时间毫秒数是否在给定的时间字符串之后。
     *
     * @param timeMillis   时间毫秒数。
     * @param afterTimeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 如果时间在之后，返回 true，否则返回 false。
     */
    public static Boolean isAfterTimeStr(Long timeMillis, String afterTimeStr) {
        Integer compared = isCompareTimeStr(timeMillis, afterTimeStr);
        if (compared != null) {
            return compared > 0;
        }
        return false;
    }

    /**
     * 检查给定的时间毫秒数是否在给定的时间字符串之前或相等。
     *
     * @param timeMillis    时间毫秒数。
     * @param beforeTimeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 如果时间在之前或相等，返回 true，否则返回 false。
     */
    public static Boolean isBeforeOrCompareTimeStr(Long timeMillis, String beforeTimeStr) {
        Integer compared = isCompareTimeStr(timeMillis, beforeTimeStr);
        if (compared != null) {
            return compared <= 0;
        }
        return false;
    }

    /**
     * 检查给定的时间毫秒数是否在给定的时间字符串之后或相等。
     *
     * @param timeMillis   时间毫秒数。
     * @param afterTimeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 如果时间在之后或相等，返回 true，否则返回 false。
     */
    public static Boolean isAfterOrCompareTimeStr(Long timeMillis, String afterTimeStr) {
        Integer compared = isCompareTimeStr(timeMillis, afterTimeStr);
        if (compared != null) {
            return compared >= 0;
        }
        return false;
    }

    /**
     * 比较给定的时间毫秒数和时间字符串。
     *
     * @param timeMillis     时间毫秒数。
     * @param compareTimeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 如果时间在之前，返回负数；如果相等，返回 0；如果之后，返回正数。
     */
    public static Integer isCompareTimeStr(Long timeMillis, String compareTimeStr) {
        try {
            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTimeInMillis(timeMillis);
            Calendar compareCalendar = getTodayCalendarByTimeStr(compareTimeStr);
            if (compareCalendar != null) {
                return timeCalendar.compareTo(compareCalendar);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return null;
    }

    /**
     * 根据时间字符串获取今天的日历对象。
     *
     * @param timeStr 时间字符串，格式为 "HH-mm-ss"。
     * @return 日历对象。
     */
    public static Calendar getTodayCalendarByTimeStr(String timeStr) {
        return getCalendarByTimeStr((Long) null, timeStr);
    }

    /**
     * 根据时间毫秒数和时间字符串获取日历对象。
     *
     * @param timeMillis 时间毫秒数。
     * @param timeStr    时间字符串，格式为 "HH-mm-ss"。
     * @return 日历对象。
     */
    public static Calendar getCalendarByTimeStr(Long timeMillis, String timeStr) {
        try {
            Calendar timeCalendar = getCalendarByTimeMillis(timeMillis);
            return getCalendarByTimeStr(timeCalendar, timeStr);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return null;
    }

    /**
     * 根据日历对象和时间字符串设置时间。
     *
     * @param timeCalendar 日历对象。
     * @param timeStr      时间字符串，格式为 "HH-mm-ss"。
     * @return 设置后的时间日历对象。
     */
    public static Calendar getCalendarByTimeStr(Calendar timeCalendar, String timeStr) {
        try {
            int length = timeStr.length();
            switch (length) {
                case 6:
                    timeCalendar.set(Calendar.SECOND, Integer.parseInt(timeStr.substring(4)));
                    timeCalendar.set(Calendar.MINUTE, Integer.parseInt(timeStr.substring(2, 4)));
                    timeCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr.substring(0, 2)));
                    break;
                case 4:
                    timeCalendar.set(Calendar.SECOND, 0);
                    timeCalendar.set(Calendar.MINUTE, Integer.parseInt(timeStr.substring(2, 4)));
                    timeCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr.substring(0, 2)));
                    break;
                case 2:
                    timeCalendar.set(Calendar.SECOND, 0);
                    timeCalendar.set(Calendar.MINUTE, 0);
                    timeCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStr.substring(0, 2)));
                    break;
                default:
                    return null;
            }
            timeCalendar.set(Calendar.MILLISECOND, 0);
            return timeCalendar;
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return null;
    }

    /**
     * 根据时间毫秒数获取日历对象。
     *
     * @param timeMillis 时间毫秒数。
     * @return 日历对象。
     */
    public static Calendar getCalendarByTimeMillis(Long timeMillis) {
        Calendar timeCalendar = Calendar.getInstance();
        if (timeMillis != null) {
            timeCalendar.setTimeInMillis(timeMillis);
        }
        return timeCalendar;
    }

    /**
     * 获取当前时间的字符串表示。
     *
     * @param ts 时间毫秒数。
     * @return 时间字符串，格式为 "HH:mm:ss"。
     */
    public static String getTimeStr(long ts) {
        return DateFormat.getTimeInstance().format(new java.util.Date(ts));
    }

    /**
     * 获取当前日期的字符串表示。
     *
     * @return 日期字符串，格式为 "yyyy-MM-dd"。
     */
    public static String getDateStr() {
        return getDateStr(0);
    }

    /**
     * 获取给定天数偏移后的日期字符串表示。
     *
     * @param plusDay 天数偏移量。
     * @return 日期字符串，格式为 "yyyy-MM-dd"。
     */
    public static String getDateStr(int plusDay) {
        Calendar c = Calendar.getInstance();
        if (plusDay != 0) {
            c.add(Calendar.DATE, plusDay);
        }
        return DateFormat.getDateInstance().format(c.getTime());
    }

    /**
     * 获取今天的日历对象。
     *
     * @return 今天的日历对象。
     */
    public static Calendar getToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /**
     * 获取当前的日历对象。
     *
     * @return 当前的日历对象。
     */
    public static Calendar getNow() {
        return Calendar.getInstance();
    }

    /**
     * 使当前线程暂停指定的毫秒数。
     *
     * @param millis 毫秒数。
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取指定时间的周数。
     *
     * @param dateTime 时间。
     * @return 当前年的第几周。
     */
    public static int getWeekNumber(Date dateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateTime);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * 比较第一个时间戳的天数是否小于第二个时间戳的天数。
     *
     * @param firstdTimestamp 第一个时间戳。
     * @param secondTimestamp 第二个时间戳。
     * @return 如果小于，则为 true，否则为 false。
     */
    public static boolean isLessThanSecondOfDays(Long firstdTimestamp, Long secondTimestamp) {
        final long gmt8 = 8 * 60 * 60 * 1000;
        final long day = 24 * 60 * 60 * 1000;
        firstdTimestamp = firstdTimestamp + gmt8;
        secondTimestamp = secondTimestamp + gmt8;
        return firstdTimestamp / day < secondTimestamp / day;
    }

    /**
     * 通过时间戳比较传入的时间戳的天数是否小于当前时间戳的天数。
     *
     * @param timestamp 时间戳。
     * @return 如果小于当前时间戳所计算的天数，则为 true，否则为 false。
     */
    public static boolean isLessThanNowOfDays(Long timestamp) {
        return isLessThanSecondOfDays(timestamp, System.currentTimeMillis());
    }

    /**
     * 获取通用的日期格式化对象。
     *
     * @return 日期格式化对象。
     */
    @SuppressLint("SimpleDateFormat")
    public static DateFormat getCommonDateFormat() {
        return new SimpleDateFormat("dd日HH:mm:ss");
    }

    /**
     * 获取通用的日期字符串表示。
     *
     * @param timestamp 时间戳。
     * @return 日期字符串，格式为 "dd日HH:mm:ss"。
     */
    @SuppressLint("SimpleDateFormat")
    public static String getCommonDate(Long timestamp) {
        return getCommonDateFormat().format(timestamp);
    }
}