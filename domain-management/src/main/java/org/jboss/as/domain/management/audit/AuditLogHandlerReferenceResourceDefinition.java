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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_HANDLER;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AuditLogHandlerReferenceResourceDefinition extends SimpleResourceDefinition {

    static final PathElement PATH_ELEMENT = PathElement.pathElement(HANDLER);

    public AuditLogHandlerReferenceResourceDefinition(ManagedAuditLogger auditLogger, boolean executeRuntime) {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver("core.management.audit-log.handler-reference"),
                new AuditLogHandlerReferenceAddHandler(auditLogger, executeRuntime), new AuditLogHandlerReferenceRemoveHandler(auditLogger, executeRuntime));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
    }

    private static class AuditLogHandlerReferenceAddHandler extends AbstractAddStepHandler {
        private final ManagedAuditLogger auditLogger;
        private final boolean executeRuntime;

        AuditLogHandlerReferenceAddHandler(ManagedAuditLogger auditLogger, boolean executeRuntime) {
            this.auditLogger = auditLogger;
            this.executeRuntime = executeRuntime;
        }

        @Override
        protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws  OperationFailedException {
            final PathAddress addr = PathAddress.pathAddress(operation.require(OP_ADDR));
            String name = addr.getLastElement().getValue();
            if (!lookForHandler(context, addr, name)) {
                throw DomainManagementMessages.MESSAGES.noHandlerCalled(name);
            }
            resource.getModel().setEmptyObject();
        }

        @Override
        protected boolean requiresRuntime(OperationContext context){
            return executeRuntime;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
                throws OperationFailedException {
            auditLogger.getUpdater().addHandlerReference(PathAddress.pathAddress(operation.require(OP_ADDR)));
        }

        @Override
        protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model,
                List<ServiceController<?>> controllers) {
            auditLogger.getUpdater().rollbackChanges();
        }

        private boolean lookForHandler(OperationContext context, PathAddress addr, String name) {
            PathAddress referenceAddress = addr.subAddress(0, addr.size() - 2).append(FILE_HANDLER, name);
            final Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
            if (lookForResource(root, referenceAddress)) {
                return true;
            }
            referenceAddress  = addr.subAddress(0, addr.size() - 2).append(SYSLOG_HANDLER, name);
            return lookForResource(root, referenceAddress);
        }

        private boolean lookForResource(final Resource root, final PathAddress pathAddress) {
            Resource current = root;
            for (PathElement element : pathAddress) {
                current = current.getChild(element);
                if (current == null) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class AuditLogHandlerReferenceRemoveHandler extends AbstractRemoveStepHandler {
        private final ManagedAuditLogger auditLogger;
        private final boolean executeRuntime;

        AuditLogHandlerReferenceRemoveHandler(ManagedAuditLogger auditLogger, boolean executeRuntime){
            this.auditLogger = auditLogger;
            this.executeRuntime = executeRuntime;

        }

        @Override
        protected boolean requiresRuntime(OperationContext context){
            return executeRuntime;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            auditLogger.getUpdater().removeHandlerReference(PathAddress.pathAddress(operation.require(OP_ADDR)));
        }

        @Override
        protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            auditLogger.getUpdater().rollbackChanges();
        }
    }
}
