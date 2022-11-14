package tech.bletchleypark.tools;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.joda.time.DateTime;
import org.quartz.CronExpression;

public class CronTools {

    /*
     * Field Name Mandatory Allowed Values Allowed Special Characters
     * Seconds YES 0-59 , - * /
     * Minutes YES 0-59 , - * /
     * Hours YES 0-23 , - * /
     * Day of month YES 1-31 , - * ? / L W
     * Month YES 1-12 or JAN-DEC , - * /
     * Day of week YES 1-7 or SUN-SAT , - * ? / L #
     * Year NO empty, 1970-2099 , - * /
     */
    private String seconds = "0"; //
    private String minutes = "*";
    private String hours = "*";
    private String dayOfTheMonth = "*";
    private String month = "*";
    private String dayOfTheWeek = "*";
    private String year = "";
    private String holiday = "*";
    private JLabel boundTo;

    public static class CronExpressionBuilder {

        private String seconds = "0"; //
        private String minutes = "*";
        private String hours = "*";
        private String dayOfTheMonth = "*";
        private String month = "*";
        private String dayOfTheWeek = "?";
        private String year = "";

        public CronExpressionBuilder() {
        }

        public CronExpressionBuilder(String expression) {
            parse(expression);
        }

        public CronExpressionBuilder(CronExpression expression) {
            parse(expression.getCronExpression());
        }

        @Override
        public String toString() {
            return seconds + " " + minutes + " " + hours + " " + dayOfTheMonth + " " + month + " " + dayOfTheWeek + " "
                    + ((!year.isEmpty()) ? " " + year : "");
        }

        public CronExpression build() throws ParseException {
            return new CronExpression(toString());
        }

        public void parse(String string) {
            String[] parts = string.replaceAll("  ", " ").split(" ");
            seconds = parts[0];
            minutes = parts[1];
            hours = parts[2];
            dayOfTheMonth = parts[3];
            month = parts[4];
            dayOfTheWeek = parts[5];
            if (parts.length == 7) {
                year = parts[6];
            }
        }

        public void setExpression(String cronExpression) {
            parse(cronExpression);
        }

        public CronExpressionBuilder setMinutes(String minutes) {
            this.minutes = minutes;
            return this;
        }

        public CronExpressionBuilder setHours(String hours) {
            this.hours = hours;
            return this;
        }

        public CronExpressionBuilder setMonths(String month) {
            this.month = month;
            return this;
        }

        public CronExpressionBuilder setDayOfTheMonth(String dayOfTheMonth) {
            this.dayOfTheMonth = dayOfTheMonth;
            return this;
        }

        public CronExpressionBuilder setDayOfTheWeek(String dayOfTheWeek) {
            this.dayOfTheWeek = dayOfTheWeek;
            return this;
        }
    }

    public static Calendar getNextScheduledRun(Calendar lastRan, Calendar runUntil, CronExpression cronExpression)
            throws ParseException {
        Calendar nsRun = Calendar.getInstance();
        nsRun.setTimeInMillis(lastRan.getTimeInMillis());
        CronExpression cron = new CronExpression(cronExpression);
        nsRun.setTime(cron.getNextValidTimeAfter(nsRun.getTime()));
        return (runUntil == null || nsRun.getTimeInMillis() <= runUntil.getTimeInMillis()) ? nsRun : null;
    }

    public static Calendar getNextScheduledRun(Calendar lastRan, CronExpression cronExpression) throws ParseException {
        Calendar nsRun = Calendar.getInstance();
        nsRun.setTimeInMillis(lastRan.getTimeInMillis());
        CronExpression cron = new CronExpression(cronExpression);
        nsRun.setTime(cron.getNextValidTimeAfter(nsRun.getTime()));
        return nsRun;
    }

    public static DateTime getNextScheduledRun(DateTime now, String cronExpression) throws ParseException {
        return getNextScheduledRun(now, new CronExpression(cronExpression));
    }

    public static DateTime getNextScheduledRun(DateTime lastRan, CronExpression cronExpression) throws ParseException {
        CronExpression cron = new CronExpression(cronExpression);
        return new DateTime(cron.getNextValidTimeAfter(lastRan.toDate()));

    }

    public static DateTime getNextScheduledRun(CronExpression cronExpression)
            throws ParseException {
        CronExpression cron = new CronExpression(cronExpression);
        Date next = cron.getNextValidTimeAfter(DateTime.now().toDate());
        return new DateTime(next);
    }

    public enum Minute {
        EVERY_MINUTE("*"),
        EVEN_MINUTES("*/2"),
        ODD_MINUTES("1-59/2"),
        EVERY_5_MINUTES("*/5"),
        EVERY_15_MINUTES("*/15"),
        EVERY_30_MINUTES("*/30");

        private final String value;

        private Minute(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    };

    public enum Hour {
        EVERY_HOUR("*"),
        EVEN_HOURS("*/2"),
        ODD_HOURS("1-23/2"),
        EVERY_6_HOURS("*/6"),
        EVERY_12_HOURS("*/12");

        private final String value;

        private Hour(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    };

    public enum DayOfTheMonth {
        EVERY_DAY("*"),
        EVEN_DAYS("*/2"),
        ODD_DAYS("1-31/2"),
        EVERY_5_DAYS("*/5"),
        EVERY_7_DAYS("*/7"),
        EVERY_10_DAYS("*/10"),
        EVERY_HALF_MONTH("*/15");

        private final String value;

        private DayOfTheMonth(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    };

    public enum Month {
        EVERY_MONTH("*"),
        EVEN_MONTHS("*/2"),
        ODD_MONTHS("1-12/2"),
        EVERY_4_MONTHS("*/4"),
        EVERY_HALF_YEAR("*/6");

        private final String value;

        private Month(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    };

    public enum DaysOfTheWeek {
        EVERY_DAY("*"),
        MONDAY_FRIDAY("2-6"),
        WEEKENDS("1,7");

        private final String value;

        private DaysOfTheWeek(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    };

    public enum Holiday {
        IGNORE("*"),
        DAY_BEFORE("1"),
        SKIP("0");

        private final String value;

        private Holiday(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    };

    public CronTools() {
    }

    public CronTools parse(String string) {
        String[] parts = string.split(" ");
        minutes = parts[0];
        hours = parts[1];
        dayOfTheMonth = parts[2];
        month = parts[3];
        dayOfTheWeek = parts[4];
        holiday = parts[5];
        return this;
    }

    public void setMinute(Minute minute) {
        this.minutes = minute.getValue();
        updateBoundTo();
    }

    public void setMinute(int... minute) {
        this.minutes = listToString(minute);
        updateBoundTo();
    }

    public void setHour(Hour hour) {
        this.hours = hour.getValue();
        updateBoundTo();
    }

    public void setHour(int... hour) {
        this.hours = listToString(hour);
        updateBoundTo();
    }

    public void setDayOfTheMonth(DayOfTheMonth days) {
        this.dayOfTheMonth = days.getValue();
        updateBoundTo();
    }

    public void setDayOfTheMonth(int... day) {
        this.dayOfTheMonth = listToString(day);
        updateBoundTo();
    }

    public void setMonth(Month month) {
        this.month = month.getValue();
        updateBoundTo();
    }

    public void setMonth(int... month) {
        this.month = listToString(month);
        updateBoundTo();
    }

    public void setDaysOfTheWeek(DaysOfTheWeek month) {
        this.dayOfTheWeek = month.getValue();
        updateBoundTo();
    }

    public void setDaysOfTheWeek(int... daysOfTheWeek) {
        this.dayOfTheWeek = listToString(daysOfTheWeek);
        updateBoundTo();
    }

    public void setHoliday(Holiday holiday) {
        this.holiday = holiday.getValue();
        updateBoundTo();
    }

    public static String listToString(int... values) {
        String sValue = "";
        int last = -2;
        boolean rangeValue = false;
        for (int v : values) {
            if (last + 1 == v) {
                if (!rangeValue) {
                    sValue += "-";
                }
                rangeValue = true;
            } else {
                if (rangeValue) {
                    sValue += last;
                    rangeValue = false;
                }
                sValue += (sValue.isEmpty() ? "" : ",") + v;

            }
            last = v;

        }
        if (rangeValue) {
            sValue += last;
        }
        return sValue;
    }

    public CronTools(JLabel jLabel) {
        boundTo = jLabel;
        updateBoundTo();
    }

    public void updateBoundTo() {
        SwingUtilities.invokeLater(() -> {

            boundTo.setText(toString());
        });
    }

    @Override
    public String toString() {
        return seconds + " " + minutes + " " + hours + " " + dayOfTheMonth + " " + month + " " + dayOfTheWeek + " "
                + holiday + ((!year.isEmpty()) ? " " + year : "");
    }

}