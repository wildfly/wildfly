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
package org.jboss.as.domain.management.audit;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CUSTOM_FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_HANDLER;

import java.util.HashMap;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.audit.spi.AuditLogEventFormatter;
import org.jboss.as.controller.audit.spi.CustomAuditLogEventFormatter;
import org.jboss.as.controller.audit.spi.CustomAuditLogEventFormatterFactory;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.as.domain.management.logging.DomainManagementLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CustomAuditLogFormatterResourceDefinition extends SimpleResourceDefinition {

    private final ManagedAuditLogger auditLogger;

    public static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CODE, ModelType.STRING)
        .setAllowNull(false)
        .setAllowExpression(true)
        .setMinSize(1)
        .build();

    public static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MODULE, ModelType.STRING)
        .setAllowNull(false)
        .setAllowExpression(true)
        .setMinSize(1)
        .build();


    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[]{CODE, MODULE};

    public CustomAuditLogFormatterResourceDefinition(ManagedAuditLogger auditLogger) {
        super(PathElement.pathElement(CUSTOM_FORMATTER), DomainManagementResolver.getResolver("core.management.custom-formatter"),
                new CustomAuditLogFormatterAddHandler(auditLogger), new CustomAuditLogFormatterRemoveHandler(auditLogger));
        this.auditLogger = auditLogger;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        //Make Code and attribute reload-required
        ReloadRequiredWriteAttributeHandler write = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition def : ATTRIBUTES){
            resourceRegistration.registerReadWriteAttribute(def, null, write);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new CustomAuditLogFormatterPropertyResourceDefinition(auditLogger));
    }

    private static CustomAuditLogEventFormatter createFormatter(OperationContext context, ModelNode operation, ManagedAuditLogger auditLogger, boolean rollback) throws OperationFailedException {
        final PathAddress address = getFormatterAddress(operation);
        ModelNode model = getModelForOperation(context, address, rollback);
        CustomAuditLogEventFormatterFactory factory;
        try {
            factory = SecurityActions.createAuditLogEventFormatterFactory(
                    auditLogger,
                    MODULE.resolveModelAttribute(context, model).asString(),
                    CODE.resolveModelAttribute(context, model).asString());
        } catch (Exception e) {
            throw new OperationFailedException(e);
        }


        final String name = address.getLastElement().getValue();
        final CustomAuditLogEventFormatter formatter = factory.createFormatter(name);

        if (rollback) {
            //An add will update the formatter directly, for a rolled back remove add the properties here
            HashMap<String, String> properties = new HashMap<String, String>();
            if (model.hasDefined(PROPERTY)) {
                for (Property prop : model.get(PROPERTY).asPropertyList()) {
                    String value = CustomAuditLogFormatterPropertyResourceDefinition.VALUE.resolveModelAttribute(context, prop.getValue()).asString();
                    properties.put(prop.getName(), value);
                }
            }

        }

        return formatter;
    }

    static PathAddress getFormatterAddress(ModelNode operation) {
        final PathAddress opAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        PathAddress addr = PathAddress.EMPTY_ADDRESS;
        for (PathElement element : opAddress) {
            addr = addr.append(element);
            if (element.getKey().equals(CUSTOM_FORMATTER)){
                break;
            }
        }
        return addr;
    }

    private static ModelNode getModelForOperation(OperationContext context, PathAddress address, boolean rollback) throws OperationFailedException {
        if (!address.getLastElement().getKey().equals(CUSTOM_FORMATTER)) {
            address = address.subAddress(0, address.size() - 1);
        }
        final Resource resource;
        if (rollback) {
            Resource root = context.getOriginalRootResource();
            Resource entry = root;
            for (PathElement element : address) {
                if (entry == null){
                    entry = entry.getChild(element);
                    //TODO i18n
                    throw DomainManagementLogger.ROOT_LOGGER.couldNotFindFormatter(address);
                }
            }
            resource = entry;
        } else {
            resource = context.readResourceFromRoot(address);
        }
        return Resource.Tools.readModel(resource);
    }

    private static class CustomAuditLogFormatterAddHandler extends AbstractAddStepHandler {

        private final ManagedAuditLogger auditLogger;

        private CustomAuditLogFormatterAddHandler(ManagedAuditLogger auditLogger) {
            super(ATTRIBUTES);
            this.auditLogger = auditLogger;
        }

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource)
                throws OperationFailedException {
            HandlerUtil.checkNoOtherFormatterWithTheSameName(context, operation);
            super.populateModel(context, operation, resource);
        }

        @Override
        protected boolean requiresRuntime(OperationContext context){
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
                throws OperationFailedException {
            AuditLogEventFormatter formatter = createFormatter(context, operation, auditLogger, false);
            auditLogger.addFormatter(formatter);
        }

        @Override
        protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model,
                List<ServiceController<?>> controllers)  {
            auditLogger.removeFormatter(Util.getNameFromAddress(operation.require(OP_ADDR)));
        }
    }

    private static class CustomAuditLogFormatterRemoveHandler extends AbstractRemoveStepHandler {
        private final ManagedAuditLogger auditLogger;

        private CustomAuditLogFormatterRemoveHandler(ManagedAuditLogger auditLogger) {
            this.auditLogger = auditLogger;
        }

        @Override
        protected void performRemove(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            final String name = Util.getNameFromAddress(operation.require(OP_ADDR));
            final Resource auditLog = context.readResourceFromRoot(PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT, AccessAuditResourceDefinition.PATH_ELEMENT));
            checkFormatterNotReferenced(name, auditLog, FILE_HANDLER, SYSLOG_HANDLER);
            super.performRemove(context, operation, model);
        }

        private void checkFormatterNotReferenced(String name, Resource auditLog, String...handlerTypes) throws OperationFailedException {
            for (String handlerType : handlerTypes) {
                for (ResourceEntry entry : auditLog.getChildren(handlerType)) {
                    ModelNode auditLogModel = entry.getModel();
                    if (auditLogModel.get(FORMATTER).asString().equals(name)) {
                        throw DomainManagementLogger.ROOT_LOGGER.cannotRemoveReferencedFormatter(entry.getPathElement());
                    }
                }
            }
        }

        @Override
        protected boolean requiresRuntime(OperationContext context){
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            auditLogger.removeFormatter(Util.getNameFromAddress(operation.require(OP_ADDR)));
        }

        @Override
        protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            AuditLogEventFormatter formatter = createFormatter(context, operation, auditLogger, true);
            auditLogger.addFormatter(formatter);
        }
    }
}
