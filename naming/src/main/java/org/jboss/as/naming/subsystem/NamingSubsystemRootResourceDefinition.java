/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.naming.management.JndiViewOperation;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the Naming subsystem's root management resource.
 *
 * @author Stuart Douglas
 */
public class NamingSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    public static final NamingSubsystemRootResourceDefinition INSTANCE = new NamingSubsystemRootResourceDefinition();

    static final SimpleOperationDefinition JNDI_VIEW = new SimpleOperationDefinitionBuilder(JndiViewOperation.OPERATION_NAME, NamingExtension.getResourceDescriptionResolver(NamingExtension.SUBSYSTEM_NAME))
            .withFlag(OperationEntry.Flag.RUNTIME_ONLY)
            .build();

    private NamingSubsystemRootResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME),
                NamingExtension.getResourceDescriptionResolver(NamingExtension.SUBSYSTEM_NAME),
                NamingSubsystemAdd.INSTANCE, NamingSubsystemRemove.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }


}
