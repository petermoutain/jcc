package com.onewaveinc.mrc;

/**
 * 模块依赖项，由依赖模块 ID 和依赖模块版本范围组成
 * 
 * @author gmice
 */
public class RequireEntry {

    /**
     * 解析模块依赖项
     * 
     * @param s 待解析的字符串
     * @return 模块依赖项实例
     */
    public static RequireEntry parse(String s) {
        String[] t = s.split("/");
        if (t.length != 2) throw new RequireEntryFormatException("错误的 RequireEntry 格式: " + s);
        try {
            String id = t[0].trim();
            VersionRange versionRange = VersionRange.parse(t[1].trim());
            return new RequireEntry(id, versionRange);
        } catch (VersionRangeFormatException e) {
            throw new RequireEntryFormatException("错误的 RequireEntry 格式: " + s, e);
        }
    }

    private String id;

    private VersionRange versionRange;

    public RequireEntry(String id, VersionRange versionRange) {
        this.id = id;
        this.versionRange = versionRange;
    }

    /**
     * 获取依赖模块 ID
     * 
     * @return 依赖模块 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取依赖模块版本范围
     * 
     * @return 依赖模块版本范围
     */
    public VersionRange getVersionRange() {
        return versionRange;
    }

    @Override
    public String toString() {
        return id + "/" + versionRange;
    }

}
