/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.as.pojo;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported namespaces of the POJO subsystem.
 * @author Paul Ferraro
 */
public enum PojoSubsystemSchema implements PersistentSubsystemSchema<PojoSubsystemSchema> {

    VERSION_1_0(1),
    ;
    static final PojoSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, PojoSubsystemSchema> namespace;

    PojoSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(PojoExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, PojoSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(PojoResource.PATH, this.namespace).build();
    }
}
