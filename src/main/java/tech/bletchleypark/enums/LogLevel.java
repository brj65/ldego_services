package tech.bletchleypark.enums;

public enum LogLevel implements BletchleyparkEnum<LogLevel> {
    ALL(0), ERROR(1), WARN(2), INFO(3), DEBUG(4), VERBOSE(5);

    public final int value;

    private LogLevel(int level) {
        value = level;
    }

    public boolean isAll() {
        return this.value <= ALL.value;
    }

    public boolean isError() {
        return this.value <= ERROR.value;
    }

    public boolean isWarn() {
        return this.value <= WARN.value;
    }

    public boolean isINFO() {
        return this.value <= INFO.value;
    }

    public boolean isDebug() {
        return this.value <= DEBUG.value;
    }

    public boolean isVerbose() {
        return this.value <= VERBOSE.value;
    }

    public LogLevel defaultValue() {
        return LogLevel.WARN;
    }

    public boolean logThis(LogLevel logLevel ) {
        return logLevel.value <= this.value;
    }
}
