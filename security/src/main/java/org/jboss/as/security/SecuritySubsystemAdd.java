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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.security.CommonAttributes.AUDIT_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.AUTHENTICATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.AUTHORIZATION_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.DEEP_COPY_SUBJECT_MODE;
import static org.jboss.as.security.CommonAttributes.DEFAULT_CALLBACK_HANDLER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.IDENTITY_TRUST_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.MAPPING_MANAGER_CLASS_NAME;
import static org.jboss.as.security.CommonAttributes.SECURITY_DOMAIN;
import static org.jboss.as.security.CommonAttributes.SUBJECT_FACTORY_CLASS_NAME;

import javax.naming.Context;
import javax.naming.Reference;
import javax.security.auth.login.Configuration;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.naming.service.JavaContextService;
import org.jboss.as.security.context.SecurityDomainObjectFactory;
import org.jboss.as.security.processors.SecurityDependencyProcessor;
import org.jboss.as.security.service.JaasBinderService;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.as.security.service.SecurityBootstrapService;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Values;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.auth.callback.JBossCallbackHandler;
import org.jboss.security.auth.login.XMLLoginConfigImpl;
import org.jboss.security.plugins.JBossAuthorizationManager;
import org.jboss.security.plugins.JBossSecuritySubjectFactory;
import org.jboss.security.plugins.audit.JBossAuditManager;
import org.jboss.security.plugins.auth.JaasSecurityManagerBase;
import org.jboss.security.plugins.identitytrust.JBossIdentityTrustManager;
import org.jboss.security.plugins.mapping.JBossMappingManager;

/**
 * Add Security Subsystem Operation.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry
 */
class SecuritySubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    private static final String AUTHENTICATION_MANAGER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot()
            + ":" + JaasSecurityManagerBase.class.getName();

    private static final String CALLBACK_HANDLER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot() + ":"
            + JBossCallbackHandler.class.getName();

    private static final String AUTHORIZATION_MANAGER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot()
            + ":" + JBossAuthorizationManager.class.getName();

    private static final String AUDIT_MANAGER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot() + ":"
            + JBossAuditManager.class.getName();

    private static final String IDENTITY_TRUST_MANAGER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot()
            + ":" + JBossIdentityTrustManager.class.getName();

    private static final String MAPPING_MANAGER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot() + ":"
            + JBossMappingManager.class.getName();

    private static final boolean DEFAULT_DEEP_COPY_OPERATION_MODE = false;

    private static final String SUBJECT_FACTORY = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot() + ":"
            + JBossSecuritySubjectFactory.class.getName();

    static final SecuritySubsystemAdd INSTANCE = new SecuritySubsystemAdd();

    /** Private to ensure singleton. */
    private SecuritySubsystemAdd() {
    }

    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
        // Create the compensating operation
        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));

        String authenticationManagerClassName = "default";
        String callbackHandlerClassName = "default";
        boolean deepCopySubject = DEFAULT_DEEP_COPY_OPERATION_MODE;
        String subjectFactoryClassName = "default";
        String authorizationManagerClassName = "default";
        String auditManagerClassName = "default";
        String identityTrustManagerClassName = "default";
        String mappingManagerClassName = "default";

        final ModelNode subModel = context.getSubModel();
        if (operation.hasDefined(AUTHENTICATION_MANAGER_CLASS_NAME)) {
            authenticationManagerClassName = operation.get(AUTHENTICATION_MANAGER_CLASS_NAME).asString();
            subModel.get(AUTHENTICATION_MANAGER_CLASS_NAME).set(authenticationManagerClassName);
        }
        if (operation.hasDefined(DEEP_COPY_SUBJECT_MODE)) {
            deepCopySubject = operation.get(DEEP_COPY_SUBJECT_MODE).asBoolean();
            subModel.get(DEEP_COPY_SUBJECT_MODE).set(deepCopySubject);
        }
        if (operation.hasDefined(DEFAULT_CALLBACK_HANDLER_CLASS_NAME)) {
            callbackHandlerClassName = operation.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME).asString();
            subModel.get(DEFAULT_CALLBACK_HANDLER_CLASS_NAME).set(callbackHandlerClassName);
        }
        if (operation.hasDefined(SUBJECT_FACTORY_CLASS_NAME)) {
            subjectFactoryClassName = operation.get(SUBJECT_FACTORY_CLASS_NAME).asString();
            subModel.get(SUBJECT_FACTORY_CLASS_NAME).set(subjectFactoryClassName);
        }
        if (operation.hasDefined(AUTHORIZATION_MANAGER_CLASS_NAME)) {
            authorizationManagerClassName = operation.get(AUTHORIZATION_MANAGER_CLASS_NAME).asString();
            subModel.get(AUTHORIZATION_MANAGER_CLASS_NAME).set(authorizationManagerClassName);
        }
        if (operation.hasDefined(AUDIT_MANAGER_CLASS_NAME)) {
            auditManagerClassName = operation.get(AUDIT_MANAGER_CLASS_NAME).asString();
            subModel.get(AUDIT_MANAGER_CLASS_NAME).set(auditManagerClassName);
        }
        if (operation.hasDefined(IDENTITY_TRUST_MANAGER_CLASS_NAME)) {
            identityTrustManagerClassName = operation.get(IDENTITY_TRUST_MANAGER_CLASS_NAME).asString();
            subModel.get(IDENTITY_TRUST_MANAGER_CLASS_NAME).set(identityTrustManagerClassName);
        }
        if (operation.hasDefined(MAPPING_MANAGER_CLASS_NAME)) {
            mappingManagerClassName = operation.get(MAPPING_MANAGER_CLASS_NAME).asString();
            subModel.get(MAPPING_MANAGER_CLASS_NAME).set(mappingManagerClassName);
        }
        subModel.get(SECURITY_DOMAIN).setEmptyObject();

        if (context instanceof BootOperationContext) {
            final BootOperationContext updateContext = (BootOperationContext) context;

            updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MODULE,
                    new SecurityDependencyProcessor());

            final ServiceTarget target = updateContext.getServiceTarget();

            // add bootstrap service
            final SecurityBootstrapService bootstrapService = new SecurityBootstrapService();
            target.addService(SecurityBootstrapService.SERVICE_NAME, bootstrapService).setInitialMode(
                    ServiceController.Mode.ACTIVE).install();

            // add service to bind SecurityDomainObjectFactory to JNDI
            final Reference reference = SecurityDomainObjectFactory.createReference("JSM");
            final JaasBinderService binderService = new JaasBinderService(Values.immediateValue(reference));
            target.addService(JaasBinderService.SERVICE_NAME, binderService).addDependency(JavaContextService.SERVICE_NAME,
                    Context.class, binderService.getContextInjector()).setInitialMode(ServiceController.Mode.ACTIVE).install();

            // add security management service
            if ("default".equals(authenticationManagerClassName)) {
                authenticationManagerClassName = AUTHENTICATION_MANAGER;
            }
            if ("default".equals(callbackHandlerClassName)) {
                callbackHandlerClassName = CALLBACK_HANDLER;
            }
            if ("default".equals(authorizationManagerClassName)) {
                authorizationManagerClassName = AUTHORIZATION_MANAGER;
            }
            if ("default".equals(auditManagerClassName)) {
                auditManagerClassName = AUDIT_MANAGER;
            }
            if ("default".equals(identityTrustManagerClassName)) {
                identityTrustManagerClassName = IDENTITY_TRUST_MANAGER;
            }
            if ("default".equals(mappingManagerClassName)) {
                mappingManagerClassName = MAPPING_MANAGER;
            }

            final SecurityManagementService securityManagementService = new SecurityManagementService(
                    authenticationManagerClassName, deepCopySubject, callbackHandlerClassName, authorizationManagerClassName,
                    auditManagerClassName, identityTrustManagerClassName, mappingManagerClassName);
            target.addService(SecurityManagementService.SERVICE_NAME, securityManagementService).setInitialMode(
                    ServiceController.Mode.ACTIVE).install();

            // add subject factory service
            if ("default".equals(subjectFactoryClassName))
                subjectFactoryClassName = SUBJECT_FACTORY;

            final SubjectFactoryService subjectFactoryService = new SubjectFactoryService(subjectFactoryClassName);
            target.addService(SubjectFactoryService.SERVICE_NAME, subjectFactoryService).addDependency(
                    SecurityManagementService.SERVICE_NAME, ISecurityManagement.class,
                    subjectFactoryService.getSecurityManagementInjector()).setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

            // add jaas configuration service
            Configuration loginConfig = XMLLoginConfigImpl.getInstance();
            final JaasConfigurationService jaasConfigurationService = new JaasConfigurationService(loginConfig);
            target.addService(JaasConfigurationService.SERVICE_NAME, jaasConfigurationService).setInitialMode(
                    ServiceController.Mode.ACTIVE).install();
        }

        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOperation);
    }

}
