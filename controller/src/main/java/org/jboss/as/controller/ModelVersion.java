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

package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public final class ModelVersion implements ModelVersionRange {

    private final int major;
    private final int minor;
    private final int micro;

    ModelVersion(int major, int minor, int micro) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModelVersion that = (ModelVersion) o;

        if (major != that.major) return false;
        if (micro != that.micro) return false;
        if (minor != that.minor) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + micro;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(major).append(".").append(minor).append(".").append(micro);
        return builder.toString();
    }

    public ModelNode toModelNode() {
        final ModelNode node = new ModelNode();
        addToExistingModel(node);
        return node;
    }

    public void addToExistingModel(final ModelNode node) {
        node.get(MANAGEMENT_MAJOR_VERSION).set(major);
        node.get(MANAGEMENT_MINOR_VERSION).set(minor);
        node.get(MANAGEMENT_MICRO_VERSION).set(micro);
    }

    @Override
    public ModelVersion[] getVersions() {
        return new ModelVersion[] { this };
    }

    public static ModelVersion create(final int major) {
        return create(major, 0, 0);
    }

    public static ModelVersion create(final int major, final int minor) {
        return create(major, minor, 0);
    }

    public static ModelVersion create(final int major, final int minor, final int micro) {
        return new ModelVersion(major, minor, micro);
    }

    public static ModelVersion fromString(final String s) {
        return convert(s);
    }

    static ModelVersion convert(final String version) {
        final String[] s = version.split("\\.");
        final int length = s.length;
        if(length > 3) {
            throw new IllegalStateException();
        }
        int major = Integer.valueOf(s[0]);
        int minor = length > 1 ? Integer.valueOf(s[1]) : 0;
        int micro = length == 3 ? Integer.valueOf(s[2]) : 0;
        return ModelVersion.create(major, minor, micro);
    }

}
