package tech.bletchleypark.enums;

public enum AssetClass {
    JPG(1, "jpg"), PNG(2, "png"), HEIC(3, "heic"), OTHER(99, "");

    public final int classId;
    public final String extention;

    private AssetClass(int id, String extention) {
        this.classId = id;
        this.extention = extention;
    }

    public static AssetClass getValueOf(String assetClass) {
        if (assetClass != null) {
            try {
                if (assetClass.contains(".")) {
                    assetClass = assetClass.substring(assetClass.lastIndexOf(".") + 1);
                }
                return valueOf(assetClass.toUpperCase());
            } catch (Exception ex) {
            }
        }
        return OTHER;
    }
}
