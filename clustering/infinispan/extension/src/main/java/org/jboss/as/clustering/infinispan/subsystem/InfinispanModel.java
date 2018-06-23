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
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.Model;
import org.jboss.as.controller.ModelVersion;

/**
 * Enumerates the supported model versions.
 * @author Paul Ferraro
 */
public enum InfinispanModel implements Model {

    VERSION_1_6_0(1, 6, 0), // EAP 6.4
    VERSION_2_0_0(2, 0, 0), // WildFly 8
    VERSION_3_0_0(3, 0, 0), // WildFly 9
    VERSION_4_0_0(4, 0, 0), // WildFly 10, EAP 7.0
    VERSION_4_1_0(4, 1, 0), // WildFly 10.1
    VERSION_5_0_0(5, 0, 0), // WildFly 11, EAP 7.1
    VERSION_6_0_0(6, 0, 0), // WildFly 12
    VERSION_7_0_0(7, 0, 0), // WildFly 13
    VERSION_8_0_0(8, 0, 0), // WildFly 14
    ;
    static final InfinispanModel CURRENT = VERSION_8_0_0;

    private final ModelVersion version;

    InfinispanModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
