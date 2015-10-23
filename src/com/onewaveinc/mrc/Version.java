package com.onewaveinc.mrc;

import java.util.regex.Pattern;

/**
 * 模块版本
 * 
 * @author gmice
 */
public class Version implements Comparable<Version> {

    /**
     * 解析模块版本
     * 
     * @param s 待解析的字符串
     * @return 模块版本实例
     */
    public static Version parse(String s) {
        if (s != null) {
            String[] parts = s.split("\\.");
            if (parts.length == 3 || parts.length == 4) {
                Version version = new Version();
                try {
                    version.setMajor(Integer.parseInt(parts[0]));
                    version.setMinor(Integer.parseInt(parts[1]));
                    version.setMicro(Integer.parseInt(parts[2]));
                } catch (NumberFormatException e) {
                    throw new VersionFormatException("错误的 Version 格式: " + s, e);
                }
                version.setQualifier(parts.length == 4 ? parts[3] : null);
                return version;
            }
        }
        throw new VersionFormatException("错误的 Version 格式: " + s);
    }

    private static final Pattern HOTFIX_PATTERN = Pattern.compile("^[Hh]\\d+$");

    private Integer major;

    private Integer minor;

    private Integer micro;

    private String qualifier;

    public int compareTo(Version anotherVersion) {
        int result;
        result = major.compareTo(anotherVersion.major);
        if (result == 0) {
            result = minor.compareTo(anotherVersion.minor);
            if (result == 0) {
                result = micro.compareTo(anotherVersion.micro);
                if (result == 0) {
                    if (qualifier == null) {
                        if (anotherVersion.qualifier == null) return 0;
                        return HOTFIX_PATTERN.matcher(anotherVersion.qualifier).matches() ? -1 : 1;
                    } else {
                        if (anotherVersion.qualifier != null) return qualifier.compareTo(anotherVersion.qualifier);
                        return HOTFIX_PATTERN.matcher(qualifier).matches() ? 1 : -1;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Version)) return false;
        Version anotherVersion = (Version) obj;
        return major == anotherVersion.major && minor == anotherVersion.minor && micro == anotherVersion.micro
                && (qualifier == null ? anotherVersion.qualifier == null : qualifier.equals(anotherVersion.qualifier));
    }

    public Integer getMajor() {
        return major;
    }

    public Integer getMicro() {
        return micro;
    }

    public Integer getMinor() {
        return minor;
    }

    public String getQualifier() {
        return qualifier;
    }

    @Override
    public int hashCode() {
        return _toString().hashCode();
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public void setMicro(Integer micro) {
        this.micro = micro;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public String toString() {
        return _toString();
    }

    private String _toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(major).append('.').append(minor).append('.').append(micro);
        if (qualifier != null) {
            builder.append('.').append(qualifier);
        }
        return builder.toString();
    }

}
