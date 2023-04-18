/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemModel;

/**
 * Enumerates the supported versions of the Undertow subsystem model.
 * @author Paul Ferraro
 */
public enum UndertowSubsystemModel implements SubsystemModel {

    VERSION_11_0_0(11), // WildFly 23-26.x, EAP 7.4.x
    VERSION_12_0_0(12), // WildFly 27
    VERSION_13_0_0(13), // WildFly 28-present
    ;
    static final UndertowSubsystemModel CURRENT = VERSION_13_0_0;

    private final ModelVersion version;

    UndertowSubsystemModel(int major) {
        this(major, 0, 0);
    }

    UndertowSubsystemModel(int major, int minor) {
        this(major, minor, 0);
    }

    UndertowSubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
