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
package org.jboss.as.host.controller.operations;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.util.Locale;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;

import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.as.platform.mbean.PlatformMBeanConstants;
import org.jboss.as.platform.mbean.RootPlatformMBeanResource;
import org.jboss.dmr.ModelNode;

/**
 * The handler to add the local host definition to the DomainModel.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LocalHostAddHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "add-host";

    private final LocalHostControllerInfoImpl hostControllerInfo;

    public static LocalHostAddHandler getInstance(final LocalHostControllerInfoImpl hostControllerInfo) {
        return new LocalHostAddHandler(hostControllerInfo);
    }

    private LocalHostAddHandler(final LocalHostControllerInfoImpl hostControllerInfo) {
        this.hostControllerInfo = hostControllerInfo;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        // This is a private operation, so this op will not be called
        return new ModelNode();
    }

    /**
     * {@inheritDoc}
     */
    public void execute(OperationContext context, ModelNode operation) {
        if (!context.isBooting()) {
            throw MESSAGES.invocationNotAllowedAfterBoot(OPERATION_NAME);
        }

        final Resource rootResource = context.createResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = rootResource.getModel();
        HostModelUtil.initCoreModel(model);

        // Create the empty management security resources
        context.createResource(PathAddress.pathAddress(PathElement.pathElement(CORE_SERVICE, MANAGEMENT)));

        // Wire in the platform mbean resources. We're bypassing the context.createResource API here because
        // we want to use our own resource type. But it's ok as the createResource calls above have taken the lock
        rootResource.registerChild(PlatformMBeanConstants.ROOT_PATH, new RootPlatformMBeanResource());

        final String localHostName = operation.require(NAME).asString();
        model.get(NAME).set(localHostName);

        hostControllerInfo.setLocalHostName(localHostName);

        context.completeStep();
    }
}
