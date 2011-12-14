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

import java.util.List;

import javax.security.auth.login.Configuration;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.security.context.SecurityDomainJndiInjectable;
import org.jboss.as.security.deployment.JaccEarDeploymentProcessor;
import org.jboss.as.security.deployment.SecurityDependencyProcessor;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.as.security.service.SecurityBootstrapService;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.auth.callback.JBossCallbackHandler;
import org.jboss.security.auth.login.XMLLoginConfigImpl;
import org.jboss.security.authentication.JBossCachedAuthenticationManager;
import org.jboss.security.plugins.JBossAuthorizationManager;
import org.jboss.security.plugins.JBossSecuritySubjectFactory;
import org.jboss.security.plugins.audit.JBossAuditManager;
import org.jboss.security.plugins.identitytrust.JBossIdentityTrustManager;
import org.jboss.security.plugins.mapping.JBossMappingManager;

/**
 * @author Jason T. Greene
 */
public class SecuritySubsystemRootResourceDefinition extends SimpleResourceDefinition {

    public static final SecuritySubsystemRootResourceDefinition INSTANCE = new SecuritySubsystemRootResourceDefinition();
    public static final SimpleAttributeDefinition DEEP_COPY_SUBJECT_MODE =
            new SimpleAttributeDefinitionBuilder(Constants.DEEP_COPY_SUBJECT_MODE, ModelType.BOOLEAN, true)
                    .setDefaultValue(new ModelNode().set(false)).build();

    private SecuritySubsystemRootResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SecurityExtension.SUBSYSTEM_NAME),
                SecurityExtension.getResourceDescriptionResolver(SecurityExtension.SUBSYSTEM_NAME), NewSecuritySubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
         resourceRegistration.registerReadWriteAttribute(DEEP_COPY_SUBJECT_MODE, null, new ReloadRequiredWriteAttributeHandler(DEEP_COPY_SUBJECT_MODE));
    }

    static class NewSecuritySubsystemAdd extends AbstractBoottimeAddStepHandler {
        private static final String AUTHENTICATION_MANAGER = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot()
                + ":" + JBossCachedAuthenticationManager.class.getName();

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

        private static final String SUBJECT_FACTORY = ModuleName.PICKETBOX.getName() + ":" + ModuleName.PICKETBOX.getSlot() + ":"
                + JBossSecuritySubjectFactory.class.getName();

        private static final Logger log = Logger.getLogger("org.jboss.as.security");
        public static final OperationStepHandler INSTANCE = new NewSecuritySubsystemAdd();


        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            DEEP_COPY_SUBJECT_MODE.validateAndSet(operation, model);
        }

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
            log.info("Activating Security Subsystem");

            final ServiceTarget target = context.getServiceTarget();

            final SecurityBootstrapService bootstrapService = new SecurityBootstrapService();
            newControllers.add(target.addService(SecurityBootstrapService.SERVICE_NAME, bootstrapService)
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE).install());

            context.addStep(new AbstractDeploymentChainStep() {
                protected void execute(DeploymentProcessorTarget processorTarget) {

                }
            }, OperationContext.Stage.RUNTIME);

            // add service to bind SecurityDomainJndiInjectable to JNDI
            final SecurityDomainJndiInjectable securityDomainJndiInjectable = new SecurityDomainJndiInjectable();
            final BinderService binderService = new BinderService("jaas");
            newControllers.add(target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("jaas"), binderService)
                .addInjection(binderService.getManagedObjectInjector(), securityDomainJndiInjectable)
                .addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class, securityDomainJndiInjectable.getSecurityManagementInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE).install());

            // add security management service
            ModelNode modelNode = model.get(Constants.DEEP_COPY_SUBJECT_MODE);
            final SecurityManagementService securityManagementService = new SecurityManagementService(
                AUTHENTICATION_MANAGER, modelNode.isDefined() && modelNode.asBoolean(), CALLBACK_HANDLER,
                AUTHORIZATION_MANAGER, AUDIT_MANAGER, IDENTITY_TRUST_MANAGER, MAPPING_MANAGER);
            newControllers.add(target.addService(SecurityManagementService.SERVICE_NAME, securityManagementService)
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE).install());

            // add subject factory service
            final SubjectFactoryService subjectFactoryService = new SubjectFactoryService(SUBJECT_FACTORY);
            newControllers.add(target.addService(SubjectFactoryService.SERVICE_NAME, subjectFactoryService)
                .addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class,
                        subjectFactoryService.getSecurityManagementInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE).install());

            // add jaas configuration service
            Configuration loginConfig = XMLLoginConfigImpl.getInstance();
            final JaasConfigurationService jaasConfigurationService = new JaasConfigurationService(loginConfig);
            newControllers.add(target.addService(JaasConfigurationService.SERVICE_NAME, jaasConfigurationService)
                .addListener(verificationHandler).setInitialMode(ServiceController.Mode.ACTIVE).install());

            newControllers.add(target.addService(SimpleSecurityManagerService.SERVICE_NAME, new SimpleSecurityManagerService())
                .addListener(verificationHandler).install());

            context.addStep(new AbstractDeploymentChainStep() {
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JACC_POLICY,
                            new JaccEarDeploymentProcessor());
                    processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_SECURITY,
                            new SecurityDependencyProcessor());
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

}
