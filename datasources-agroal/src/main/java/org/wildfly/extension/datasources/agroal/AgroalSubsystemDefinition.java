/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Definition for the Agroal subsystem
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class AgroalSubsystemDefinition extends PersistentResourceDefinition {

    static final AgroalSubsystemDefinition INSTANCE = new AgroalSubsystemDefinition();

    private static final List<PersistentResourceDefinition> CHILDREN = unmodifiableList(asList(DataSourceDefinition.INSTANCE, XADataSourceDefinition.INSTANCE, DriverDefinition.INSTANCE));

    private AgroalSubsystemDefinition() {
        super(pathElement(SUBSYSTEM, AgroalExtension.SUBSYSTEM_NAME), AgroalExtension.getResolver(), AgroalSubsystemOperations.ADD_OPERATION, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return emptyList();
    }

    @Override
    public List<PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }
}
