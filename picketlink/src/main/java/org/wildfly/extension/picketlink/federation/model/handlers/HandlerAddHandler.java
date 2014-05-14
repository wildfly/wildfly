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

package org.wildfly.extension.picketlink.federation.model.handlers;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.picketlink.config.federation.KeyValueType;
import org.picketlink.config.federation.handler.Handler;
import org.wildfly.extension.picketlink.federation.service.EntityProviderService;
import org.wildfly.extension.picketlink.federation.service.IdentityProviderService;
import org.wildfly.extension.picketlink.federation.service.SAMLHandlerService;
import org.wildfly.extension.picketlink.federation.service.ServiceProviderService;
import org.wildfly.extension.picketlink.logging.PicketLinkLogger;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_HANDLER_PARAMETER;
import static org.wildfly.extension.picketlink.common.model.ModelElement.IDENTITY_PROVIDER;
import static org.wildfly.extension.picketlink.federation.service.SAMLHandlerService.createServiceName;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class HandlerAddHandler extends AbstractAddStepHandler {

    static final HandlerAddHandler INSTANCE = new HandlerAddHandler();

    private HandlerAddHandler() {

    }

    static void launchServices(final OperationContext context, final PathAddress pathAddress , final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        Handler newHandler = new Handler();

        ModelNode classNameNode = HandlerResourceDefinition.CLASS.resolveModelAttribute(context, model);
        ModelNode codeNode = HandlerResourceDefinition.CODE.resolveModelAttribute(context, model);
        String typeName;

        if (classNameNode.isDefined()) {
            typeName = classNameNode.asString();
        } else if (codeNode.isDefined()) {
            typeName = HandlerTypeEnum.forType(codeNode.asString());
        } else {
            throw PicketLinkLogger.ROOT_LOGGER.federationHandlerTypeNotProvided();
        }

        newHandler.setClazz(typeName);

        ModelNode handler = Resource.Tools.readModel(context.readResourceFromRoot(pathAddress));

        if (handler.hasDefined(COMMON_HANDLER_PARAMETER.getName())) {
            for (Property handlerParameter : handler.get(COMMON_HANDLER_PARAMETER.getName()).asPropertyList()) {
                String paramName = handlerParameter.getName();
                String paramValue = HandlerParameterResourceDefinition.VALUE
                                        .resolveModelAttribute(context, handlerParameter.getValue()).asString();

                KeyValueType kv = new KeyValueType();

                kv.setKey(paramName);
                kv.setValue(paramValue);

                newHandler.add(kv);
            }
        }

        SAMLHandlerService service = new SAMLHandlerService(newHandler);
        PathElement providerAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement();

        ServiceTarget serviceTarget = context.getServiceTarget();
        ServiceBuilder<SAMLHandlerService> serviceBuilder = serviceTarget.addService(createServiceName(providerAlias
            .getValue(), newHandler.getClazz()), service);
        ServiceName serviceName;

        if (providerAlias.getKey().equals(IDENTITY_PROVIDER.getName())) {
            serviceName = IdentityProviderService.createServiceName(providerAlias.getValue());
        } else {
            serviceName = ServiceProviderService.createServiceName(providerAlias.getValue());
        }

        serviceBuilder.addDependency(serviceName, EntityProviderService.class, service.getEntityProviderService());

        if (verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }

        ServiceController<SAMLHandlerService> controller = serviceBuilder.setInitialMode(ServiceController.Mode.PASSIVE).install();

        if (newControllers != null) {
            newControllers.add(controller);
        }

        if (!context.isBooting()) {
            // a reload is required to get the chain properly updated with the domain model state.
            context.reloadRequired();
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition attribute : HandlerResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
                                         List<ServiceController<?>> newControllers) throws OperationFailedException {
        launchServices(context, PathAddress.pathAddress(operation.get(ADDRESS)), model, verificationHandler, newControllers);
    }
}