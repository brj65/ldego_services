package lde.kiwi.mfiles;

public class MfilesAlias {
    public String vault;
    public String alias;
    public long mfilesId;

    public MfilesAlias(String vault, String alias, long mfilesId) {
        this.vault = vault;
        this.alias = alias;
        this.mfilesId = mfilesId;
    }
}