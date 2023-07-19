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

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported Undertow subsystem schemas.
 * @author Paul Ferraro
 */
public enum UndertowSubsystemSchema implements PersistentSubsystemSchema<UndertowSubsystemSchema> {
/*  Unsupported, for documentation purposes only
    VERSION_1_0(1, 0),  // WildFly 8.0
    VERSION_1_1(1, 1),  // WildFly 8.1
    VERSION_1_2(1, 2),  // WildFly 8.2
    VERSION_2_0(2),     // WildFly 9
    VERSION_3_0(3, 0),  // WildFly 10.0
 */
    VERSION_3_1(3, 1),  // WildFly 10.1
    VERSION_4_0(4),     // WildFly 11
    VERSION_5_0(5),     // WildFly 12
    VERSION_6_0(6),     // WildFly 13
    VERSION_7_0(7),     // WildFly 14
    VERSION_8_0(8),     // WildFly 15-16
    VERSION_9_0(9),     // WildFly 17
    VERSION_10_0(10),   // WildFly 18-19
    VERSION_11_0(11),   // WildFly 20-22    N.B. There were no parser changes between 10.0 and 11.0 !!
    VERSION_12_0(12),   // WildFly 23-26.1, EAP 7.4
    VERSION_13_0(13),   // WildFly 27       N.B. There were no schema changes between 12.0 and 13.0!
    VERSION_14_0(14),   // WildFly 28-present
    ;
    static final UndertowSubsystemSchema CURRENT = VERSION_14_0;

    private final VersionedNamespace<IntVersion, UndertowSubsystemSchema> namespace;

    UndertowSubsystemSchema(int major) {
        this(new IntVersion(major));
    }

    UndertowSubsystemSchema(int major, int minor) {
        this(new IntVersion(major, minor));
    }

    UndertowSubsystemSchema(IntVersion version) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(UndertowExtension.SUBSYSTEM_NAME, version);
    }

    @Override
    public VersionedNamespace<IntVersion, UndertowSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return UndertowPersistentResourceXMLDescriptionFactory.INSTANCE.apply(this);
    }
}
