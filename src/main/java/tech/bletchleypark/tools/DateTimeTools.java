package tech.bletchleypark.tools;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import tech.bletchleypark.enums.DateTimeUnit;

public class DateTimeTools {

    public static final org.joda.time.format.DateTimeFormatter STD_TIME_FORMATTER = DateTimeFormat
            .forPattern("kk:mm:ss");
    public static final org.joda.time.format.DateTimeFormatter STD_TIME_FORMATTER_ZONE = DateTimeFormat
            .forPattern("kk:mm:ss z");
    public static final org.joda.time.format.DateTimeFormatter STD_DATE_FORMATTER = DateTimeFormat
            .forPattern("EEE dd MMM");
    public static final org.joda.time.format.DateTimeFormatter STD_DATE_TIME_FORMATTER = DateTimeFormat
            .forPattern("EEE dd MMM yy kk:mm");
    public static final org.joda.time.format.DateTimeFormatter STD_DATE_TIME_FORMATTER_ZONE = DateTimeFormat
            .forPattern("EEE dd MMM yy kk:mm z");

    public static DateTimeZone getSystemTimeZone() {
        if (System.getProperty("slipstream.timezone") == null) {
            return DateTimeZone.getDefault();
        }
        return DateTimeZone.forID(System.getProperty("slipstream.timezone"));
    }

    public static void setSystemTimeZone(DateTimeZone dateTimeZone) {
        DateTimeZone.forID(System.setProperty("slipstream.timezone", dateTimeZone.getID()));
    }

    public static Timestamp convertStringToTimestamp(String strDate) {
        try {
            DateFormat formatter = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
            // you can change format of date
            Date date = formatter.parse(strDate);
            Timestamp timeStampDate = new Timestamp(date.getTime());

            return timeStampDate;
        } catch (ParseException e) {
            System.out.println("Exception :" + e);
            return null;
        }
    }

    public static DateTime timeStampToDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new DateTime(timestamp.getTime());
    }

    public static DateTime timeStampToDateTime(Timestamp timestamp, TimeZone timeZone) {
        return new DateTime(timestamp.getTime(), DateTimeZone.forID(timeZone.getID()));
    }

    public static Timestamp dateTimeToTimestamp(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        dateTime = dateTime.toDateTime(getSystemTimeZone());
        return new Timestamp(dateTime.toDateTime(DateTimeZone.getDefault()).getMillis());
    }

    public static DateTime dateTimeRemoveSeconds(DateTime dateTime) {
        return dateTime.withTime(dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), 0, 0);
    }

    public static DateTime dateTime(java.sql.Date date, Time time, DateTimeZone timezone) {
        DateTime dateTime = new DateTime(timezone);
        if (date != null) {
            int year = date.toLocalDate().getYear();
            if (date.toLocalDate().getYear() < 2000) {
                year = DateTime.now().getYear();
            }
            dateTime = dateTime.withDate(year, date.toLocalDate().getMonthValue(), date.toLocalDate().getDayOfMonth());
        }
        if (time != null) {
            dateTime = dateTime.withTime(time.toLocalTime().getHour(), time.toLocalTime().getMinute(), 0, 0);
        } else {
            dateTime = dateTime.withTime(0, 0, 0, 0);
        }
        return dateTime;
    }

    public static DateTime stringToDateTime(String string) {
        return DateTime.parse(string.replace(" ", "T"));
    }

    public static boolean isTomorrow(DateTime dateTime) {
        boolean afterRollover = DateTime.now().getHourOfDay() > 3
                || (DateTime.now().getHourOfDay() == 3 && DateTime.now().getMinuteOfDay() > 0);
        return dateTime.isAfter(DateTime.now().plus(afterRollover ? 1 : 0));
    }

    public static DateTime dateToDateTime(Date date) {
        return new DateTime(date);
    }

    public static class DateRange {

        public final DateTime start;
        public final DateTime end;

        public DateRange(DateTime start, DateTime end) {
            this.start = start;
            this.end = end;
        }

    }

    public static DateRange DateRange(DateTime start, DateTime end) {
        return new DateRange(start, end);
    }

    @Deprecated
    public static DateRange DateRange(Calendar start, Calendar end) {
        return new DateRange(calendarToDateTime(start), calendarToDateTime(end));
    }

    public static boolean defaultTimeZone(Object value) {
        switch (value.getClass().getSimpleName()) {
            case "DateTime":
                return ((DateTime) value).getZone() == DateTimeZone.forTimeZone(getSystemTimeZone().toTimeZone());

            default:
                return false;
        }

    }

    public static LocalTime calendarToLocalTime(Calendar cal) {
        return new LocalTime(cal.getTimeInMillis());
    }

    public static DateTime calendarToDateTime(Calendar cal) {
        return new DateTime(cal.getTimeInMillis(), DateTimeZone.forTimeZone(cal.getTimeZone()));
    }

    public static DateTime calendarToDateTimeConfigTimeZone(Calendar cal) {
        return calendarToDateTime(cal).withZone(DateTimeZone.forTimeZone(getSystemTimeZone().toTimeZone()));
    }

    public static DateTime nowUsingConfigTimeZone() {
        return DateTime.now(DateTimeZone.forTimeZone(getSystemTimeZone().toTimeZone()));
    }

    public static String dateTimeSTDDateTimeFormat(DateTime dateTime, boolean withTimeZone) {
        if (dateTime.getZone() != DateTimeZone.forTimeZone(getSystemTimeZone().toTimeZone()) || withTimeZone) {
            return dateTime.toString(STD_DATE_TIME_FORMATTER_ZONE);
        } else {
            return dateTime.toString(STD_DATE_TIME_FORMATTER);
        }
    }

    public static String dateTimeSTDDateFormat(DateTime dateTime) {
        return dateTime.toString(STD_DATE_FORMATTER);
    }

    public static String dateTimeSTDTimeFormat(DateTime dateTime, boolean withTimeZone) {
        if (dateTime.getZone() != DateTimeZone.forTimeZone(getSystemTimeZone().toTimeZone()) || withTimeZone) {
            return dateTime.toString(STD_TIME_FORMATTER_ZONE);
        } else {
            return dateTime.toString(STD_TIME_FORMATTER);
        }
    }

    public static String dateTimeSTDTimeFormatNoTimeZone(DateTime dateTime) {
        return dateTime.toString(STD_TIME_FORMATTER);
    }

    public static Timestamp CalendarToTimeStampUTC(Calendar calendar) {
        return new Timestamp(calendarToUTC(calendar).getTimeInMillis());
    }

    public static Timestamp calendarToTimeStamp(Calendar calendar) {
        return new Timestamp(calendar.getTimeInMillis());
    }

    public static Calendar TimeStampToCalendar(Timestamp timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp.getTime());
        return c;
    }

    public static Calendar localDateTimeToCalendar(LocalDateTime localDateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(localDateTime.getYear(), localDateTime.getMonthValue() - 1, localDateTime.getDayOfMonth(),
                localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond());
        return calendar;
    }

    // public static Calendar calendarSetDate(Calendar cal, Date date) {
    // cal.set(Calendar.YEAR, date.getYear());
    // cal.set(Calendar.MONTH, date.getMonth());
    // cal.set(Calendar.DATE, date.getDate());
    // return cal;
    // }
    public static Calendar calendarSetDate(Calendar calTime, Calendar calDateToSet) {
        if (!calTime.getTimeZone().equals(calDateToSet.getTimeZone())) {
            int offset = calTime.getTimeZone().getRawOffset() - calDateToSet.getTimeZone().getRawOffset();
            calDateToSet.add(Calendar.MILLISECOND, offset);
        }
        calTime.set(Calendar.YEAR, calDateToSet.get(Calendar.YEAR));
        calTime.set(Calendar.MONTH, calDateToSet.get(Calendar.MONTH));
        calTime.set(Calendar.DATE, calDateToSet.get(Calendar.DATE));
        return calTime;
    }

    public static Calendar calendarToUTC(Calendar calendar) {
        Calendar utcCal = Calendar.getInstance(calendar.getTimeZone());
        utcCal.setTime(calendar.getTime());
        utcCal.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcCal;
    }

    private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ");

    public static Calendar dateToCalendar(Date date) {

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssZ");
            ZonedDateTime dt = ZonedDateTime.parse(dateTimeFormat.format(date), formatter);
            ZoneId zone = dt.getZone();
            TimeZone timeZone = TimeZone.getTimeZone(zone);
            Calendar calendar = Calendar.getInstance(timeZone);
            calendar.setTimeInMillis(date.getTime());
            return calendar;
        } catch (Exception ex) {
            Logger.getLogger(DateTimeTools.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * <p>
     * Checks if two dates are on the same day ignoring time.
     * </p>
     *
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either date is <code>null</code>
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }

    /**
     * <p>
     * Checks if two dates are on the same day ignoring time.
     * </p>
     *
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either date is <code>null</code>
     */
    public static boolean isSameDayAndTime(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDayAndTime(cal1, cal2);
    }

    /**
     * <p>
     * Checks if two calendars represent the same day ignoring time.
     * </p>
     *
     * @param cal1 the first calendar, not altered, not null
     * @param cal2 the second calendar, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either calendar is <code>null</code>
     */
    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    /**
     * <p>
     * Checks if two calendars represent the same day ignoring time.
     * </p>
     *
     * @param cal1 the first calendar, not altered, not null
     * @param cal2 the second calendar, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either calendar is <code>null</code>
     */
    public static boolean isSameDayAndTime(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            return (cal1 == null && cal2 == null);
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                && cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY)
                && cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE)
                && cal1.get(Calendar.SECOND) == cal2.get(Calendar.SECOND));
    }

    /**
     * <p>
     * Checks if a date is today.
     * </p>
     *
     * @param date the date, not altered, not null.
     * @return true if the date is today.
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isToday(Date date) {
        return isSameDay(date, Calendar.getInstance().getTime());
    }

    /**
     * <p>
     * Checks if a calendar date is today.
     * </p>
     *
     * @param cal the calendar, not altered, not null
     * @return true if cal date is today
     * @throws IllegalArgumentException if the calendar is <code>null</code>
     */
    public static boolean isToday(Calendar cal) {
        return isSameDay(cal, Calendar.getInstance());
    }

    /**
     * <p>
     * Checks if the first date is before the second date ignoring time.
     * </p>
     *
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return true if the first date day is before the second date day.
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isBeforeDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isBeforeDay(cal1, cal2);
    }

    /**
     * <p>
     * Checks if the first calendar date is before the second calendar date
     * ignoring time.
     * </p>
     *
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return true if cal1 date is before cal2 date ignoring time.
     * @throws IllegalArgumentException if either of the calendars are
     *                                  <code>null</code>
     */
    public static boolean isBeforeDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        if (cal1.get(Calendar.ERA) < cal2.get(Calendar.ERA)) {
            return true;
        }
        if (cal1.get(Calendar.ERA) > cal2.get(Calendar.ERA)) {
            return false;
        }
        if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) {
            return true;
        }
        if (cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) {
            return false;
        }
        return cal1.get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * <p>
     * Checks if the first date is after the second date ignoring time.
     * </p>
     *
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return true if the first date day is after the second date day.
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isAfterDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isAfterDay(cal1, cal2);
    }

    /**
     * <p>
     * Checks if the first calendar date is after the second calendar date
     * ignoring time.
     * </p>
     *
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return true if cal1 date is after cal2 date ignoring time.
     * @throws IllegalArgumentException if either of the calendars are
     *                                  <code>null</code>
     */
    public static boolean isAfterDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        if (cal1.get(Calendar.ERA) < cal2.get(Calendar.ERA)) {
            return false;
        }
        if (cal1.get(Calendar.ERA) > cal2.get(Calendar.ERA)) {
            return true;
        }
        if (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) {
            return false;
        }
        if (cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) {
            return true;
        }
        return cal1.get(Calendar.DAY_OF_YEAR) > cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * <p>
     * Checks if a date is after today and within a number of days in the
     * future.
     * </p>
     *
     * @param date the date to check, not altered, not null.
     * @param days the number of days.
     * @return true if the date day is after today and within days in the future
     *         .
     * @throws IllegalArgumentException if the date is <code>null</code>
     */
    public static boolean isWithinDaysFuture(Date date, int days) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return isWithinDaysFuture(cal, days);
    }

    /**
     * <p>
     * Checks if a calendar date is after today and within a number of days in
     * the future.
     * </p>
     *
     * @param cal  the calendar, not altered, not null
     * @param days the number of days.
     * @return true if the calendar date day is after today and within days in
     *         the future .
     * @throws IllegalArgumentException if the calendar is <code>null</code>
     */
    public static boolean isWithinDaysFuture(Calendar cal, int days) {
        if (cal == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar today = Calendar.getInstance();
        Calendar future = Calendar.getInstance();
        future.add(Calendar.DAY_OF_YEAR, days);
        return (isAfterDay(cal, today) && !isAfterDay(cal, future));
    }

    /**
     * Returns the given date with the time set to the start of the day.
     */
    public static Date getStart(Date date) {
        return clearTime(date);
    }

    /**
     * Returns the given date with the time values cleared.
     */
    public static Date clearTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Determines whether or not a date has any time values (hour, minute,
     * seconds or millisecondsReturns the given date with the time values
     * cleared.
     */
    /**
     * Determines whether or not a date has any time values.
     *
     * @param date The date.
     * @return true iff the date is not null and any of the date's hour, minute,
     *         seconds or millisecond values are greater than zero.
     */
    public static boolean hasTime(Date date) {
        if (date == null) {
            return false;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if (c.get(Calendar.HOUR_OF_DAY) > 0) {
            return true;
        }
        if (c.get(Calendar.MINUTE) > 0) {
            return true;
        }
        if (c.get(Calendar.SECOND) > 0) {
            return true;
        }
        if (c.get(Calendar.MILLISECOND) > 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns the given date with time set to the end of the day
     */
    public static Date getEnd(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    /**
     * Returns the maximum of two dates. A null date is treated as being less
     * than any non-null date.
     */
    public static Date max(Date d1, Date d2) {
        if (d1 == null && d2 == null) {
            return null;
        }
        if (d1 == null) {
            return d2;
        }
        if (d2 == null) {
            return d1;
        }
        return (d1.after(d2)) ? d1 : d2;
    }

    /**
     * Returns the minimum of two dates. A null date is treated as being greater
     * than any non-null date.
     */
    public static Date min(Date d1, Date d2) {
        if (d1 == null && d2 == null) {
            return null;
        }
        if (d1 == null) {
            return d2;
        }
        if (d2 == null) {
            return d1;
        }
        return (d1.before(d2)) ? d1 : d2;
    }

    /**
     * The maximum date possible.
     */
    public static Date MAX_DATE = new Date(Long.MAX_VALUE);
    public static SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd KK:mm:ss.SSS");

    public static String toSQL(Calendar calendar) {
        return sqlFormat.format(calendar.getTime());
    }

    public static String toSQL(DateTime dateTime) {
        return dateTime.toString(sqlFormat.toPattern());
    }

    public static Date fromStringToDate(String dateString) throws ParseException {
        return fromStringToDate(dateString, "yyyy-MM-dd KK:mm:ss.S");
    }

    public static Date fromStringToDate(String dateString, String format) throws ParseException {
        return new SimpleDateFormat(format).parse(dateString);
    }

    public static Calendar fromStringToCalendar(String dateString) throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromStringToDate(dateString));
        return cal;
    }

    public static Calendar fromYYYYMMDDToCalendar(String dateString) throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromStringToDate(dateString, "yyyy/MM/dd"));
        return cal;
    }

    public static String formatDateAsUTCFormat(Calendar dateTime) {
        return String.format("%1$FT%1$tR", dateTime);
    }

    public static boolean isALeapYear(int year) {
        return (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0));
    }

    public static String getTimeHHMM(DateTime dateTime1, DateTime dateTime2) {
        int diff = (int) (dateTime2.getMillis() - dateTime1.getMillis()) / 1000;
        if (diff <= 0) {
            return "00:00";
        }
        int hours = diff / (60 * 60);
        int minutes = (diff - (hours * (60 * 60))) / 60;
        return String.format("%3s", hours) + ":" + String.format("%02d", minutes);
    }

    public static double getTimeMinutes(DateTime dateTime1, DateTime dateTime2) {
        return (getTimeSeconds(dateTime1, dateTime2) / 60.0);
    }

    public static int getTimeSeconds(DateTime dateTime1, DateTime dateTime2) {
        return (int) (dateTime2.getMillis() - dateTime1.getMillis()) / 1000;
    }

    public static DateTime getDateTime(String dateString) {
        switch (dateString.trim().toLowerCase()) {
            case "today", "now":
                return DateTime.now();
            case "tomorrow":
                return DateTime.now().plusDays(1);
            case "yesterday":
                return DateTime.now().minusDays(1);
            case "week":
                return DateTime.now().minusDays(7);
            case "month":
                return DateTime.now().minusMonths(1);
            default:
                return new DateTime(dateString);
        }
    }

    public static DateTime dateTimePlus(TimeUnit unit, int amount) {
        return switch (unit) {
            case DAYS -> DateTime.now().plusYears(amount);
            case HOURS -> DateTime.now().plusHours(amount);
            case MINUTES -> DateTime.now().plusMinutes(amount);
            case SECONDS -> DateTime.now().plusSeconds(amount);
            default -> DateTime.now();
        };
    }

    public static DateTime dateTimePlus(DateTime dateTime, DateTimeUnit unit, int amount) {
        return switch (unit) {
            case SECONDS -> dateTime.plusSeconds(amount);
            case MINUTES -> dateTime.plusMinutes(amount);
            case HOURS -> dateTime.plusHours(amount);
            case DAYS -> dateTime.plusDays(amount);
            case WEEKS -> dateTime.plusWeeks(amount);
            case MONTHS -> dateTime.plusMonths(amount);
            case YEARS -> dateTime.plusYears(amount);
            default -> null;
        };
    }

    public static DateTime getDateTime(int amount, DateTimeUnit unit) {
        switch (unit) {
            case SECONDS:
                return DateTime.now().plusSeconds(amount);
            case HOURS:
                return DateTime.now().plusDays(amount);
            case DAYS:
                return DateTime.now().plusDays(amount);
            case WEEKS:
                return DateTime.now().plusDays(amount);
            case MONTHS:
                return DateTime.now().plusMonths(amount);
            case YEARS:
                return DateTime.now().plusYears(amount);
            default:
                return null;
        }
    }
}
