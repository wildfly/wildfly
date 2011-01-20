/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import org.jboss.as.controller.operations.common.AddNamespaceHandler;
import org.jboss.as.controller.operations.common.AddSchemaLocationHandler;
import org.jboss.as.controller.operations.common.RemoveNamespaceHandler;
import org.jboss.as.controller.operations.common.RemoveSchemaLocationHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.registry.ModelNodeRegistration;

/**
 * Records core operations with the host controller model registry.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class CoreOperationsRegistrar {

    /**
     * Prevent instantiation
     */
    private CoreOperationsRegistrar() {
    }

    static void registerCoreOperations(ModelNodeRegistration root) {
        root.registerReadWriteAttribute(NAME, null, new StringLengthValidatingHandler(1));
        root.registerOperationHandler(AddNamespaceHandler.OPERATION_NAME, AddNamespaceHandler.INSTANCE, AddNamespaceHandler.INSTANCE, false);
        root.registerOperationHandler(RemoveNamespaceHandler.OPERATION_NAME, RemoveNamespaceHandler.INSTANCE, RemoveNamespaceHandler.INSTANCE, false);
        root.registerOperationHandler(AddSchemaLocationHandler.OPERATION_NAME, AddSchemaLocationHandler.INSTANCE, AddSchemaLocationHandler.INSTANCE, false);
        root.registerOperationHandler(RemoveSchemaLocationHandler.OPERATION_NAME, RemoveSchemaLocationHandler.INSTANCE, RemoveSchemaLocationHandler.INSTANCE, false);
    }

}
