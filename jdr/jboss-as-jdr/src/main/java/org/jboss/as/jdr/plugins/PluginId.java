/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
