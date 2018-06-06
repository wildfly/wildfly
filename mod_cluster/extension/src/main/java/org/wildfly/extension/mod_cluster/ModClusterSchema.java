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

import java.util.Locale;

import org.jboss.as.clustering.controller.Schema;

/**
 * @author Jean-Frederic Clere
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public enum ModClusterSchema implements Schema<ModClusterSchema> {

    MODCLUSTER_1_0(1, 0), // AS 7.0
    MODCLUSTER_1_1(1, 1), // EAP 6.0 & 6.1 & 6.2
    MODCLUSTER_1_2(1, 2), // EAP 6.3 & 6.4
    MODCLUSTER_2_0(2, 0), // WildFly 10, EAP 7.0
    MODCLUSTER_3_0(3, 0), // WildFly 11 & 12 & 13, EAP 7.1
    MODCLUSTER_4_0(4, 0), // WildFly 14
    ;
    public static final ModClusterSchema CURRENT = MODCLUSTER_4_0;

    private final int major;
    private final int minor;

    ModClusterSchema(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    @Override
    public int major() {
        return this.major;
    }

    @Override
    public int minor() {
        return this.minor;
    }

    @Override
    public String getNamespaceUri() {
        return String.format(Locale.ROOT, "urn:jboss:domain:%s:%d.%d", ModClusterExtension.SUBSYSTEM_NAME, this.major, this.minor);
    }
}
