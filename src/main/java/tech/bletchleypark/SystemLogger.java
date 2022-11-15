package tech.bletchleypark;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.bletchleypark.enums.LogLevel;
import tech.bletchleypark.tools.Function;
import static tech.bletchleypark.ApplicationLifecycle.*;

public class SystemLogger {

    private LogLevel level;
    private static List<LogLine> logLines = new java.util.ArrayList<>();
    private static boolean updating;
    //
    private final Class<?> aClass;
    private final int serviceId;
    private boolean localLogOnly = false;

    public enum ErrorCode {
        GENERAL(1000),
        PROCESS_STILL_RUNNING(2000);

        public final int code;

        private ErrorCode(int errorCode) {
            this.code = errorCode;
        }
    }

    private SystemLogger(Class<?> aClass) {
        this(aClass, 0);
    }

    private SystemLogger(Class<?> aClass, Integer serviceId) {
        this(aClass, serviceId, LogLevel.valueOf(ConfigProviderManager.optConfigString("quarkus.log.level", "INFO")));
    }

    private SystemLogger(Class<?> aClass, Integer serviceId, LogLevel logLevel) {
        this.aClass = aClass;
        this.serviceId = serviceId;
        level = logLevel;
    }

    public static SystemLogger getLogger(Class<?> aClass) {
        return new SystemLogger(aClass);
    }

    public static SystemLogger getLogger(Class<?> aClass, int serviceId) {
        return new SystemLogger(aClass, serviceId);
    }

    public static SystemLogger getLogger(Class<?> aClass, int serviceId, LogLevel logLevel) {
        return new SystemLogger(aClass, serviceId, logLevel);
    }

    public SystemLogger localLogOnly() {
        localLogOnly = true;
        return this;
    }

    public SystemLogger logLevel(LogLevel logLevel) {
        if (logLevel != null)
            level = logLevel;
        return this;
    }

    public LogLevel getLogLevel() {
        return level;
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void debug(String message, String extra) {
        log(LogLevel.DEBUG, message, extra);
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void info(String message, String extra) {
        log(LogLevel.INFO, message, extra);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public void log(LogLevel logLevel, String message) {
        log(logLevel, message, false);
    }

    public void log(LogLevel logLevel, String message, boolean force) {
        log(logLevel, message, null, force);
    }

    public void log(LogLevel logLevel, String message, String extra) {
        log(logLevel, message, extra, false);
    }

    public Builder builder() {
        return new Builder(this);
    }

    public static class Builder {
        private SystemLogger logger;

        private LogLevel logLevel;
        private String message;
        private String extra;
        private ErrorCode errorCode = ErrorCode.GENERAL;

        private boolean force;

        public Builder(SystemLogger logger) {
            this.logger = logger;
        }

        public void log() {
            logger.log(logLevel, errorCode, message, extra, force);
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder extra(String extra) {
            this.extra = extra;
            return this;
        }

        public Builder errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder force() {
            this.force = true;
            return this;
        }

        public Builder logLevel(LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder info() {
            this.logLevel = LogLevel.INFO;
            return this;
        }

        public Builder warn() {
            this.logLevel = LogLevel.WARN;
            return this;
        }
    }

    public void systemLog(LogLevel logLevel, String message) {
        Logger logger = LoggerFactory.getLogger(aClass);
        switch (logLevel) {
            case ALL -> logger.info(message);
            case ERROR -> logger.error(message);
            case WARN -> logger.warn(message);
            case INFO -> logger.info(message);
            case DEBUG -> logger.debug(message);
            case VERBOSE -> logger.info(message);
            default -> logger.warn(message);
        }
    }

    public void log(LogLevel logLevel, String message, String extra, boolean force) {
        log(logLevel, ErrorCode.GENERAL, message, extra, force);
    }

    public void log(LogLevel logLevel, ErrorCode errorCode, String message, String extra, boolean force) {
        if (!force && !level.logThis(logLevel))
            return;
        systemLog(logLevel, message);

        if (!localLogOnly && ConfigProviderManager.optConfigBoolean("bpark.log.to.database", true)) {
            logLines.add(new LogLine(aClass, logLevel, errorCode, message, extra));
            updateDatabse();
        }
    }

    /*
     * private void serverLog(int serviceId, String message) {
     * log.info("Service:" + serviceId + " - " + message);
     * try {
     * try (Connection cn = db.getConnection();
     * PreparedStatement unlock = cn
     * .prepareStatement(queryUpdateLog);) {
     * unlock.setInt(1, serviceId);
     * unlock.setTimestamp(2,
     * new Timestamp(DateTime.now().getMillis()));
     * unlock.setString(3, message == null ? "-" : message);
     * unlock.executeUpdate();
     * }
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * }
     */
    private synchronized void updateDatabse() {
        if (updating)
            return;
        updating = true;
        do {
            try {
                try (Connection connection = application.defaultDataSource.getConnection();) {
                    for (LogLine line : new ArrayList<>(logLines)) {
                        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO system_log " +
                                "(instance_id,service_id,class,level,code,message,extra) VALUES (?,?,?,?,?,?,?)")) {
                            ps.setInt(1, line.instanceId);
                            ps.setInt(2, line.serviceId);
                            ps.setString(3, line.className);
                            ps.setString(4, line.logLevel.toString());
                            ps.setInt(5, line.errorCode.code);
                            ps.setString(6, line.message == null ? "?" : line.message);
                            ps.setString(7, line.extra);
                            ps.executeUpdate();
                            if (ps.getUpdateCount() > 0)
                                logLines.remove(line);
                        }
                    }
                }
            } catch (Exception e) {
                // logger.error(e);
                e.printStackTrace();
                Function.pause(60);
            }
        } while (!logLines.isEmpty());
        updating = false;
    }

    private class LogLine {

        public final int instanceId;
        public final int serviceId = SystemLogger.this.serviceId;
        public final String className;
        public final LogLevel logLevel;
        public final String message;
        public final String extra;
        public final ErrorCode errorCode;

        public LogLine(Class<?> aClass, LogLevel logLevel, ErrorCode errorCode, String message, String extra) {
            this.className = aClass.getName();
            this.logLevel = logLevel;
            this.errorCode = errorCode;
            this.message = message;
            this.extra = extra;
            instanceId = application.getInstanceId();
        }

    }

    public void error(Throwable throwable) {
        error(throwable, null);
    }

    public void error(Throwable throwable, String extra) {
        if (!level.logThis(LogLevel.ERROR))
            return;
        Logger logger = LoggerFactory.getLogger(aClass);
        logger.error(throwable.getMessage(), throwable);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        logLines.add(new LogLine(aClass, LogLevel.ERROR, ErrorCode.GENERAL, throwable.getMessage(),
                (extra != null ? extra + "\n------------------ Stack Trace ------------------" : "") + sw.toString()));
        if (ConfigProviderManager.optConfigBoolean("bpark.log.to.database", true)) {
            updateDatabse();
        }
    }

    public void error(String string) {
        error(new Exception(string));
    }

    public void error(String string, String extra) {
        error(new Exception(string), extra);
    }

}