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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.picketlink.config.federation.STSType;
import org.wildfly.extension.picketlink.federation.service.FederationService;
import org.wildfly.extension.picketlink.federation.service.SAMLService;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class SAMLAddHandler extends AbstractAddStepHandler {

    static final SAMLAddHandler INSTANCE = new SAMLAddHandler();

    private SAMLAddHandler() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : SAMLResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        launchServices(context, operation, model);
    }

    static void launchServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        String federationAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();
        SAMLService service = new SAMLService(toSAMLConfig(context, model));
        context.getServiceTarget().addService(SAMLService.createServiceName(federationAlias), service)
                .addDependency(FederationService.createServiceName(federationAlias), FederationService.class, service.getFederationService())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();
    }

    static STSType toSAMLConfig(OperationContext context, ModelNode fromModel) throws OperationFailedException {
        int tokenTimeout = SAMLResourceDefinition.TOKEN_TIMEOUT.resolveModelAttribute(context, fromModel).asInt();
        int clockSkew = SAMLResourceDefinition.CLOCK_SKEW.resolveModelAttribute(context, fromModel).asInt();

        STSType stsType = new STSType();

        stsType.setTokenTimeout(tokenTimeout);
        stsType.setClockSkew(clockSkew);

        return stsType;
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            SAMLRemoveHandler.INSTANCE.performRuntime(context, operation, resource.getModel());
        } catch (OperationFailedException ignore) {
        }
    }
}
