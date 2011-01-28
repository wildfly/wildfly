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

package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.security.CommonAttributes.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.CommonAttributes.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;

import javax.naming.Context;
import javax.naming.Reference;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.naming.service.JavaContextService;
import org.jboss.as.security.context.SecurityDomainObjectFactory;
import org.jboss.as.security.service.JaasBinderService;
import org.jboss.as.security.service.SecurityBootstrapService;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Values;
import org.jboss.security.ISecurityManagement;

/**
 * Add Security Subsystem Operation.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class NewSecuritySubsystemAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    private static final String DEFAULT_AUTHENTICATION_MANAGER = "org.jboss.security.plugins.auth.JaasSecurityManagerBase";
    private static final boolean DEFAULT_DEEP_COPY_OPERATION_MODE = false;
    private static final String DEFAULT_CALLBACK_HANDLER = "org.jboss.security.auth.callback.JBossCallbackHandler";

    static final NewSecuritySubsystemAdd INSTANCE = new NewSecuritySubsystemAdd();

    private NewSecuritySubsystemAdd() {
        // Private to ensure a singleton.
    }

    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
        // Create the compensating operation
        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        String authenticationManagerClassName = has(operation, AUTHENTICATION_MANAGER_CLASS_NAME) ? operation.get(
                AUTHENTICATION_MANAGER_CLASS_NAME).asString() : DEFAULT_AUTHENTICATION_MANAGER;
        boolean deepCopySubjectMode = has(operation, DEEP_COPY_SUBJECT_MODE) ? operation.get(DEEP_COPY_SUBJECT_MODE).asBoolean()
                : DEFAULT_DEEP_COPY_OPERATION_MODE;
        String defaultCallbackHandlerClassName = has(operation, DEFAULT_CALLBACK_HANDLER_CLASS_NAME) ? operation.get(
                DEFAULT_CALLBACK_HANDLER_CLASS_NAME).asString() : DEFAULT_CALLBACK_HANDLER;

        if (context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;
            final ServiceTarget target = updateContext.getServiceTarget();

            // add bootstrap service
            final SecurityBootstrapService bootstrapService = new SecurityBootstrapService();
            target.addService(SecurityBootstrapService.SERVICE_NAME, bootstrapService)
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();

            // add security management service
            final SecurityManagementService securityManagementService = new SecurityManagementService(
                    authenticationManagerClassName, deepCopySubjectMode, defaultCallbackHandlerClassName);
            target.addService(SecurityManagementService.SERVICE_NAME, securityManagementService)
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();

            // add service to bind SecurityDomainObjectFactory to JNDI
            final Reference reference = SecurityDomainObjectFactory.createReference("JSM");
            final JaasBinderService binderService = new JaasBinderService(Values.immediateValue(reference));
            target.addService(JaasBinderService.SERVICE_NAME, binderService)
                    .addDependency(JavaContextService.SERVICE_NAME, Context.class, binderService.getContextInjector())
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();

            // add subject factory service
            final SubjectFactoryService subjectFactoryService = new SubjectFactoryService();
            target.addService(SubjectFactoryService.SERVICE_NAME, subjectFactoryService)
                    .addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class,
                            subjectFactoryService.getSecurityManagementInjector())
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();
        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(AUTHENTICATION_MANAGER_CLASS_NAME).set(operation.get(AUTHENTICATION_MANAGER_CLASS_NAME));
        subModel.get(DEEP_COPY_SUBJECT_MODE).set(operation.get(DEEP_COPY_SUBJECT_MODE));
        subModel.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME).set(operation.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME));

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

    private static boolean has(ModelNode node, String name) {
        return node.has(name) && node.get(name).isDefined();
    }
}
