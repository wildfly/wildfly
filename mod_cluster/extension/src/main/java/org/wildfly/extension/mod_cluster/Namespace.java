/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.wildfly.extension.mod_cluster;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

import java.util.List;

/**
 * @author Jean-Frederic Clere
 * @author Paul Ferraro
 */
public enum Namespace {
    // must be first
    UNKNOWN(ModClusterExtension.SUBSYSTEM_NAME, 0, 0, null),

    MOD_CLUSTER_1_0(ModClusterExtension.LEGACY_SUBSYSTEM_NAME, 1, 0, new ModClusterSubsystemXMLReader_1_0()),
    MOD_CLUSTER_1_1(ModClusterExtension.LEGACY_SUBSYSTEM_NAME, 1, 1, new ModClusterSubsystemXMLReader_1_1()),
//    MOD_CLUSTER_2_0(ModClusterExtension.SUBSYSTEM_NAME, 2, 0, new ModClusterSubsystemXMLReader_2_0()),
    ;
    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = MOD_CLUSTER_1_1;

    private final String subsystemName;
    private final int major;
    private final int minor;
    private final XMLElementReader<List<ModelNode>> reader;

    private Namespace(String subsystemName, int major, int minor, XMLElementReader<List<ModelNode>> reader) {
        this.subsystemName = subsystemName;
        this.major = major;
        this.minor = minor;
        this.reader = reader;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    public String getUri() {
        return String.format("urn:jboss:domain:%s:%d.%d", this.subsystemName, this.major, this.minor);
    }

    public String getSubsystemName() {
        return this.subsystemName;
    }

    public XMLElementReader<List<ModelNode>> getXMLReader() {
        return this.reader;
    }
}
