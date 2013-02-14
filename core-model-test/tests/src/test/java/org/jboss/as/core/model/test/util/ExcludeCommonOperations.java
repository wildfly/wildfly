/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.core.model.test.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_OVERLAY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ExcludeCommonOperations {

    private ExcludeCommonOperations() {
    }

    public static void excludeBadOps_7_1_x(LegacyKernelServicesInitializer initializer) {
        //deployment overlays don't exist in 7.1.x
        initializer.addOperationValidationExclude(ADD, PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY)));
        initializer.addOperationValidationExclude(ADD, PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT_OVERLAY), PathElement.pathElement(CONTENT)));
        initializer.addOperationValidationExclude(ADD, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP), PathElement.pathElement(DEPLOYMENT_OVERLAY)));

        //Socket binding group/socket-binding has problems if there are expressions in the multicast-port
        initializer.addOperationValidationResolve(ADD, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP), PathElement.pathElement(SOCKET_BINDING)));

        //Deployment operation validator thinks that content is required
        initializer.addOperationValidationExclude(ADD, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP), PathElement.pathElement(DEPLOYMENT)));
    }
}
