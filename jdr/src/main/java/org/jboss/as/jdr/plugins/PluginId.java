/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.plugins;

/**
 * @author csams@redhat.com
 *         Date: 11/9/12
 */
public final class PluginId implements Comparable<PluginId> {

    final String name;
    final int major;
    final int minor;
    final String release;

    final String idString;

    public PluginId(String name, int major, int minor, String release) {
        this.name = name;
        this.major = major;
        this.minor = minor;
        this.release = release;
        StringBuffer sb = new StringBuffer(name).append(": ")
                .append(major).append('.')
                .append(minor);
        if (null != release) {
            sb.append('-').append(release);
        }
        idString = sb.toString();

    }

    public String getName() {
        return name;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public String getRelease() {
        return release;
    }

    @Override
    public String toString() {
        return idString;
    }

    @Override
    public int compareTo(PluginId o) {
        int result = name.compareTo(o.name);
        if (result != 0) { return result; }
        result = major - o.major;
        if (result != 0) { return result; }
        result = minor - o.minor;
        if (result != 0) { return result; }
        if (null != release) {
            return release.compareTo(o.release);
        }
        return result;
    }
}
