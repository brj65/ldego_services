package tech.bletchleypark.enums;


public enum Access {
    USER(9),
    SUPERVISOR(8),
    ADMINSTRATOR(7),
    ITUSER(1),
    DEVELOPER(0);

    private int level;

    private Access(int level) {
        this.level = level;
    }

    public boolean isAllowedAccess(Access access) {
        return access.level >= this.level;
    }

    public static Access getValueFor(String name) {
        try {
            return valueOf(name);
        } catch (Exception ex) {
        }
        return null;
    }
}
