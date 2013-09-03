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
package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_HANDLER;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.AccessAuditResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * This has subtle differences from AuditLogHandlerReferenceResourceDefinition in domain-management so it is not a duplicate
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JmxAuditLogHandlerReferenceResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(HANDLER);

    public JmxAuditLogHandlerReferenceResourceDefinition(ManagedAuditLogger auditLogger) {
        //Make a remove reload required. Handlers using more complex formatters in the future will not like being stopped and started at runtime
        super(PATH_ELEMENT, JMXExtension.getResourceDescriptionResolver("audit-log.handler"),
                new AuditLogHandlerReferenceAddHandler(auditLogger), new AuditLogHandlerReferenceRemoveHandler(auditLogger));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
    }

    private static class AuditLogHandlerReferenceAddHandler extends AbstractAddStepHandler {
        private final ManagedAuditLogger auditLogger;

        AuditLogHandlerReferenceAddHandler(ManagedAuditLogger auditLogger) {
            this.auditLogger = auditLogger;
        }

        @Override
        protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws  OperationFailedException {
            final PathAddress addr = PathAddress.pathAddress(operation.require(OP_ADDR));
            String name = addr.getLastElement().getValue();
            boolean found = context.getProcessType().isServer() ? lookForHandlerForServer(context, name) : lookForHandlerForHc(context, name);
            if (!found) {
                throw JmxMessages.MESSAGES.noHandlerCalled(name);
            }
            resource.getModel().setEmptyObject();
        }

        @Override
        protected boolean requiresRuntime(OperationContext context){
            if (context.getProcessType() != ProcessType.HOST_CONTROLLER && context.getRunningMode() == RunningMode.ADMIN_ONLY){
                return false;
            }
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
                throws OperationFailedException {
            auditLogger.getUpdater().addHandlerReference(PathAddress.pathAddress(operation.require(OP_ADDR)));
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.completeStep(new OperationContext.ResultHandler() {
                        @Override
                        public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
                            if (resultAction == ResultAction.KEEP) {
                                auditLogger.getUpdater().applyChanges();
                            }
                        }
                    });
                }
            }, Stage.RUNTIME);
        }

        @Override
        protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model,
                List<ServiceController<?>> controllers) {
            auditLogger.getUpdater().rollbackChanges();
        }

        private boolean lookForHandlerForServer(OperationContext context, String name) {
            final Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
            return lookForHandler(PathAddress.EMPTY_ADDRESS, root, name);
        }

        private boolean lookForHandlerForHc(OperationContext context, String name) {
            final Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
            for (ResourceEntry entry : root.getChildren(ModelDescriptionConstants.HOST)) {
                if (entry.getModel().isDefined()) {
                    return lookForHandler(PathAddress.pathAddress(ModelDescriptionConstants.HOST, entry.getName()), root, name);
                }
            }
            return false;
        }

        private boolean lookForHandler(PathAddress rootAddress, Resource root, String name) {
            PathAddress addr = rootAddress.append(
                                    CoreManagementResourceDefinition.PATH_ELEMENT,
                                    AccessAuditResourceDefinition.PATH_ELEMENT);
            PathAddress referenceAddress = addr.append(FILE_HANDLER, name);
            if (lookForResource(root, referenceAddress)) {
                return true;
            }
            referenceAddress  = addr.append(SYSLOG_HANDLER, name);
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

        AuditLogHandlerReferenceRemoveHandler(ManagedAuditLogger auditLogger){
            this.auditLogger = auditLogger;

        }

        @Override
        protected boolean requiresRuntime(OperationContext context){
            if (context.getProcessType() != ProcessType.HOST_CONTROLLER && context.getRunningMode() == RunningMode.ADMIN_ONLY){
                return false;
            }
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            auditLogger.getUpdater().removeHandlerReference(PathAddress.pathAddress(operation.require(OP_ADDR)));
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.completeStep(new OperationContext.ResultHandler() {
                        @Override
                        public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
                            if (resultAction == ResultAction.KEEP) {
                                auditLogger.getUpdater().applyChanges();
                            }
                        }
                    });
                }
            }, Stage.RUNTIME);
        }

        @Override
        protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            auditLogger.getUpdater().rollbackChanges();
        }
    }
}
