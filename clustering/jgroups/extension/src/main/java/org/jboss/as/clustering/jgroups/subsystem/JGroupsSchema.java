/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Locale;

import org.jboss.as.clustering.controller.Schema;

/**
 * Enumeration of the supported subsystem xml schemas.
 * @author Paul Ferraro
 */
public enum JGroupsSchema implements Schema<JGroupsSchema> {

    VERSION_1_0(1, 0), // AS 7.0
    VERSION_1_1(1, 1), // AS 7.1
    VERSION_2_0(2, 0), // WildFly 8
    VERSION_3_0(3, 0), // WildFly 9
    VERSION_4_0(4, 0), // WildFly 10
    VERSION_5_0(5, 0), // WildFly 11
    VERSION_6_0(6, 0), // WildFly 12
    ;
    public static final JGroupsSchema CURRENT = VERSION_6_0;

    private final int major;
    private final int minor;

    JGroupsSchema(int major, int minor) {
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
        return String.format(Locale.ROOT, "urn:jboss:domain:jgroups:%d.%d", this.major, this.minor);
    }
}
