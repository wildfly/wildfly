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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.platform.mbean.PlatformMBeanConstants;
import org.jboss.as.platform.mbean.RootPlatformMBeanResource;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;

/**
 * The handler to add the local host definition to the DomainModel.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LocalHostAddHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "add-host";

    private final HostControllerEnvironment hostControllerEnvironment;
    private final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry;

    public LocalHostAddHandler(final HostControllerEnvironment hostControllerEnvironment, IgnoredDomainResourceRegistry ignoredDomainResourceRegistry) {
        this.hostControllerEnvironment = hostControllerEnvironment;
        this.ignoredDomainResourceRegistry = ignoredDomainResourceRegistry;
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

        initCoreModel(model, hostControllerEnvironment);

        // Create the empty management security resources
        context.createResource(PathAddress.pathAddress(PathElement.pathElement(CORE_SERVICE, MANAGEMENT)));

        // Wire in the platform mbean resources. We're bypassing the context.createResource API here because
        // we want to use our own resource type. But it's ok as the createResource calls above have taken the lock
        rootResource.registerChild(PlatformMBeanConstants.ROOT_PATH, new RootPlatformMBeanResource());
        // Wire in the ignored-resources resource
        Resource.ResourceEntry ignoredRoot = ignoredDomainResourceRegistry.getRootResource();
        rootResource.registerChild(ignoredRoot.getPathElement(), ignoredRoot);

        // Add a step to store the HC name
        ModelNode writeNameOp = Util.getWriteAttributeOperation(operation.get(OP_ADDR), NAME, operation.get(NAME));
        context.addStep(writeNameOp, hostControllerEnvironment.getProcessNameWriteHandler(), OperationContext.Stage.IMMEDIATE);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    private static void initCoreModel(final ModelNode root, HostControllerEnvironment environment) {

        root.get(RELEASE_VERSION).set(Version.AS_VERSION);
        root.get(RELEASE_CODENAME).set(Version.AS_RELEASE_CODENAME);
        root.get(MANAGEMENT_MAJOR_VERSION).set(Version.MANAGEMENT_MAJOR_VERSION);
        root.get(MANAGEMENT_MINOR_VERSION).set(Version.MANAGEMENT_MINOR_VERSION);

        // Community uses UNDEF values
        ModelNode nameNode = root.get(PRODUCT_NAME);
        ModelNode versionNode = root.get(PRODUCT_VERSION);

        if (environment != null) {
            String productName = environment.getProductConfig().getProductName();
            String productVersion = environment.getProductConfig().getProductVersion();

            if (productName != null) {
                nameNode.set(productName);
            }
            if (productVersion != null) {
                versionNode.set(productVersion);
            }
        }

        root.get(NAME);
        root.get(NAMESPACES).setEmptyList();
        root.get(SCHEMA_LOCATIONS).setEmptyList();
        root.get(EXTENSION);
        root.get(SYSTEM_PROPERTY);
        root.get(PATH);
        root.get(CORE_SERVICE);
        root.get(SERVER_CONFIG);
        root.get(DOMAIN_CONTROLLER);
        root.get(INTERFACE);
        root.get(JVM);
        root.get(RUNNING_SERVER);
    }

}
