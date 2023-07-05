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

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * Enumerates the supported schemas of the MicroProfile LRA coordinator subsystem.
 *
 * @author Paul Ferraro
 */
public enum MicroProfileLRACoordinatorSubsystemSchema implements PersistentSubsystemSchema<MicroProfileLRACoordinatorSubsystemSchema> {
    VERSION_1_0(1),
    ;

    private final VersionedNamespace<IntVersion, MicroProfileLRACoordinatorSubsystemSchema> namespace;

    MicroProfileLRACoordinatorSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileLRACoordinatorSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(MicroProfileLRACoordinatorSubsystemDefinition.PATH, this.namespace)
            .addAttributes(MicroProfileLRACoordinatorSubsystemDefinition.ATTRIBUTES)
            .build();
    }
}