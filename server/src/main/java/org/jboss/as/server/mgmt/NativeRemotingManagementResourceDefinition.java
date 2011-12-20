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

package org.jboss.as.server.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_REMOTING_INTERFACE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.operations.NativeManagementAddHandler;
import org.jboss.as.server.operations.NativeManagementRemoveHandler;
import org.jboss.as.server.operations.NativeManagementWriteAttributeHandler;
import org.jboss.as.server.operations.NativeRemotingManagementAddHandler;
import org.jboss.as.server.operations.NativeRemotingManagementRemoveHandler;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for the Native Remoting Interface when running a standalone server.
 * (This reuses a connector from the remoting subsystem).
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NativeRemotingManagementResourceDefinition extends SimpleResourceDefinition {

    private static final PathElement RESOURCE_PATH = PathElement.pathElement(MANAGEMENT_INTERFACE, NATIVE_REMOTING_INTERFACE);

    public static final NativeRemotingManagementResourceDefinition INSTANCE = new NativeRemotingManagementResourceDefinition();

    private NativeRemotingManagementResourceDefinition() {
        super(RESOURCE_PATH,
                ServerDescriptions.getResourceDescriptionResolver("core.management.native-remoting-interface"),
                NativeRemotingManagementAddHandler.INSTANCE, NativeRemotingManagementRemoveHandler.INSTANCE,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_NONE);
    }
}
