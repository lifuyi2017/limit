package com.cigc.limit.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by sofn
 * 2018/4/25 17:07
 */
public class DateUtils {
    private static Log logger = LogFactory.getLog(DateUtils.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //获取当前时间戳
    public static String getTimeStmap(int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minutes);
        return String.valueOf(calendar.getTimeInMillis());
    }

    public static String getDateStr() {

        return sdf2.format(new Date());
    }

    public static String getDateStr(String longTime){
        return sdf2.format(new Date(Long.parseLong(longTime)));
    }
    public static String getDateStr(long longTime){
        return sdf2.format(new Date(longTime));
    }
    public static String getIndexName() {
        String dateStr = sdf.format(new Date());
        StringBuilder sb = new StringBuilder("cqct_");

        String[] arr = dateStr.split("-");
        sb.append(arr[0]);
        //检查月份
        String monthStr = null;
        switch (arr[1]) {
            case "01":
            case "02":
            case "03":
            case "04":
                monthStr = "0104";
                break;

            case "05":
            case "06":
            case "07":
            case "08":
                monthStr = "0508";
                break;

            case "09":
            case "10":
            case "11":
            case "12":
                monthStr = "0912";
                break;
        }
        sb.append(monthStr).append("_*");

        return sb.toString();
    }

    public static String getIndexNameByTime(String date) {

        StringBuilder sb = new StringBuilder("cqct_");

        String[] arr = date.split("-");
        sb.append(arr[0]);
        //检查月份
        String monthStr = null;
        switch (arr[1]) {
            case "01":
            case "02":
            case "03":
            case "04":
                monthStr = "0104";
                break;

            case "05":
            case "06":
            case "07":
            case "08":
                monthStr = "0508";
                break;

            case "09":
            case "10":
            case "11":
            case "12":
                monthStr = "0912";
                break;
        }
        sb.append(monthStr).append("_*");

        return sb.toString();
    }

    public static String getAmPm(String date){
        int i = Integer.parseInt(date.substring(11, 13));
        if (i>=0 && i<=12) return "am";
        else if(i>12&&i<=24) return "pm";
        else return "unknown";
    }

    /**
     * 获取指定天数前的时间戳
     *
     * @param days   天数
     * @param isZero 是否为零点
     * @return
     */
    public static long getMillis(int days, boolean isZero) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, days);
        if (isZero) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }
        return calendar.getTimeInMillis();
    }






    public static long getMillis(String time,int minutes){
        long mills = 0l;
        try {
            Date parse = sdf2.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parse);
            calendar.add(Calendar.MINUTE,minutes);
            mills = calendar.getTimeInMillis();
        } catch (ParseException e) {
            logger.info("时间格式错误，转换异常"+e);
        }
        return mills;
    }

    public static String getDayStr(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, days); //获取昨天日期
        return sdf.format(calendar.getTime());
    }


    /**
     * 时间字符串转换为毫秒
     *
     * @param timeStr 时间字符串
     * @return 毫秒
     */
    public static long getMillis(String timeStr) {

        long time = 0l;
        try {
            time = sdf2.parse(timeStr).getTime();
        } catch (ParseException e) {
            logger.info("时间字符串转换错误，请检查格式！", e);
        }
        return time;
    }


    /**
     * 获取当前时间撮
     *
     * @param
     * @return 毫秒
     */
    public static String getnowMillis( ) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr=String.valueOf(df.format(new Date()));
        long time = 0l;
        try {
            time = sdf2.parse(timeStr).getTime();
        } catch (ParseException e) {
        }
        return String.valueOf(time);
    }


}
