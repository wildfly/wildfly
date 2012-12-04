/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_KEY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PREFIX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRE_SIGNED_DELETE_URL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRE_SIGNED_PUT_URL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET_ACCESS_KEY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.discovery.S3Discovery;
import org.jboss.as.host.controller.discovery.S3DiscoveryResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handler for the S3 discovery resource's add operation.
 *
 * @author Farah Juma
 */
public class S3DiscoveryAddHandler extends AbstractAddStepHandler {

    public static final String OPERATION_NAME = ADD;
    private final LocalHostControllerInfoImpl hostControllerInfo;

    /**
     * Create the S3DiscoveryAddHandler.
     *
     * @param hostControllerInfo the host controller info
     */
    public S3DiscoveryAddHandler(final LocalHostControllerInfoImpl hostControllerInfo) {
        this.hostControllerInfo = hostControllerInfo;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler,
                                  final List<ServiceController<?>> newControllers) throws OperationFailedException {
        if (context.isBooting()) {
            populateHostControllerInfo(hostControllerInfo, context, model);
        } else {
            context.reloadRequired();
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final SimpleAttributeDefinition attribute : S3DiscoveryResourceDefinition.S3_DISCOVERY_ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    private Map<String, String> resolveAttributes(OperationContext context, ModelNode model) throws OperationFailedException {
        Map<String, String> resolvedAttributes = new HashMap<String, String>();
        for (final SimpleAttributeDefinition attribute : S3DiscoveryResourceDefinition.S3_DISCOVERY_ATTRIBUTES) {
            ModelNode attributeNode = attribute.resolveModelAttribute(context, model);
            String attributeValue = attributeNode.isDefined() ? attributeNode.asString() : null;
            resolvedAttributes.put(attribute.getName(), attributeValue);
        }
        return resolvedAttributes;
    }

    protected void populateHostControllerInfo(LocalHostControllerInfoImpl hostControllerInfo, OperationContext context,
            ModelNode model) throws OperationFailedException {
        Map<String, String> resolvedAttributes = resolveAttributes(context, model);
        // Create the S3Discovery option and add it to the hostControllerInfo
        S3Discovery s3DiscoveryOption = new S3Discovery(resolvedAttributes.get(ACCESS_KEY),
                resolvedAttributes.get(SECRET_ACCESS_KEY),
                resolvedAttributes.get(LOCATION),
                resolvedAttributes.get(PREFIX),
                resolvedAttributes.get(PRE_SIGNED_PUT_URL),
                resolvedAttributes.get(PRE_SIGNED_DELETE_URL));
        hostControllerInfo.addRemoteDomainControllerDiscoveryOption(s3DiscoveryOption);
    }
}
