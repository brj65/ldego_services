package lde.kiwi.mfiles;

public class MFilesVaultAlias {
    public String vault;
    public String alias;
    public long mfilesId;

    public MFilesVaultAlias(String vault, String alias, long mfilesId) {
        this.vault = vault;
        this.alias = alias;
        this.mfilesId = mfilesId;
    }
}