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

package org.wildfly.extension.picketlink.federation.model.saml;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;
import org.wildfly.extension.picketlink.federation.service.SAMLService;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class SAMLResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition TOKEN_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelElement.SAML_TOKEN_TIMEOUT.getName(), ModelType.INT, true)
        .setDefaultValue(new ModelNode(5000))
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition CLOCK_SKEW = new SimpleAttributeDefinitionBuilder(ModelElement.SAML_CLOCK_SKEW.getName(), ModelType.INT, true)
        .setDefaultValue(new ModelNode(0))
        .setAllowExpression(true)
        .build();

    public static final SAMLResourceDefinition INSTANCE = new SAMLResourceDefinition();

    private SAMLResourceDefinition() {
        super(ModelElement.SAML, ModelElement.SAML.getName(), SAMLAddHandler.INSTANCE, SAMLRemoveHandler.INSTANCE, TOKEN_TIMEOUT, CLOCK_SKEW);
    }

    @Override
    protected OperationStepHandler createAttributeWriterHandler() {
        List<SimpleAttributeDefinition> attributes = getAttributes();
        return new AbstractWriteAttributeHandler(attributes.toArray(new AttributeDefinition[attributes.size()])) {
            @Override
            protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder handbackHolder) throws OperationFailedException {
                PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

                updateConfiguration(context, pathAddress, false);

                return false;
            }

            @Override
            protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Object handback) throws OperationFailedException {
                PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

                updateConfiguration(context, pathAddress, true);
            }

            private void updateConfiguration(OperationContext context, PathAddress pathAddress, boolean rollback) throws OperationFailedException {
                String federationAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();
                ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
                ServiceController<SAMLService> serviceController =
                        (ServiceController<SAMLService>) serviceRegistry.getService(SAMLService.createServiceName(federationAlias));

                if (serviceController != null) {
                    SAMLService service = serviceController.getValue();

                    ModelNode samlNode;

                    if (!rollback) {
                        samlNode = context.readResource(PathAddress.EMPTY_ADDRESS, false).getModel();
                    } else {
                        Resource rc = context.getOriginalRootResource().navigate(pathAddress);
                        samlNode = rc.getModel();
                    }

                    service.setStsType(SAMLAddHandler.toSAMLConfig(context, samlNode));
                }
            }
        };
    }
}
