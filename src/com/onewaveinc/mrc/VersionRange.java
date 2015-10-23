package com.onewaveinc.mrc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模块范围
 * 
 * @author gmice
 */
public class VersionRange {
    
    public static final VersionRange ALL = new VersionRange(null, true, null, true);

    private static final Pattern PATTERN1 = Pattern.compile("^([\\w\\.]*)(\\+|\\-)([\\w\\.]*)$");

    private static final Pattern PATTERN2 = Pattern.compile("^([\\[\\(])([\\w\\.]*),([\\w\\.]*)([\\]\\)])$");

    private Version min, max;

    private boolean minExclusive, maxExclusive;

    public VersionRange(Version min, boolean minExclusive, Version max, boolean maxExclusive) {
        this.min = min;
        this.minExclusive = minExclusive;
        this.max = max;
        this.maxExclusive = maxExclusive;
    }

    public boolean contains(Version version) {
        if (min != null) {
            int c = min.compareTo(version);
            if (c > 0 || (minExclusive && c == 0)) return false;
        }
        if (max != null) {
            int c = max.compareTo(version);
            if (c < 0 || (maxExclusive && c == 0)) return false;
        }
        return true;
    }

    public Version getMax() {
        return max;
    }

    public Version getMin() {
        return min;
    }

    public boolean isMaxExclusive() {
        return maxExclusive;
    }

    public boolean isMinExclusive() {
        return minExclusive;
    }

    @Override
    public String toString() {
        if (max == null && !minExclusive) return min + "+";
        if (min == null && !maxExclusive) return max + "-";
        if (min != null && !minExclusive && max != null && !maxExclusive) return min + "-" + max;
        StringBuilder builder = new StringBuilder();
        builder.append(minExclusive ? '(' : '[').append(min == null ? "?" : min.toString()).append(',').append(max == null ? "?" : max.toString()).append(maxExclusive ? ')' : ']');
        return builder.toString();
    }

    public static VersionRange parse(String s) {
        Version min = null, max = null;
        Matcher matcher;
        try {
            matcher = PATTERN1.matcher(s);
            if (matcher.matches()) {
                if ("+".equals(matcher.group(2))) {
                    min = Version.parse(matcher.group(1));
                } else {
                    String t = matcher.group(3);
                    if (t == null || t.length() == 0) {
                        max = Version.parse(matcher.group(1));
                    } else {
                        min = Version.parse(matcher.group(1));
                        max = Version.parse(t);
                    }
                }
                return new VersionRange(min, false, max, false);
            }

            matcher = PATTERN2.matcher(s);
            if (matcher.matches()) {
                return new VersionRange(Version.parse(matcher.group(2)), "(".equals(matcher.group(1)), Version.parse(matcher.group(3)), ")".equals(matcher.group(4)));
            }

            try {
                Version version = Version.parse(s);
                return new VersionRange(version, false, version, false);
            } catch (VersionFormatException ignore) {}
        } catch (VersionFormatException e) {
            throw new VersionRangeFormatException("错误的 VersionRange 格式: " + s, e);
        }

        throw new VersionRangeFormatException("错误的 VersionRange 格式: " + s);
    }

}
