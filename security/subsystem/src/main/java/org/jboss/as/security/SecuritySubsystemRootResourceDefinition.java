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

import javax.security.auth.login.Configuration;
import javax.transaction.TransactionManager;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.security.context.SecurityDomainJndiInjectable;
import org.jboss.as.security.deployment.JaccEarDeploymentProcessor;
import org.jboss.as.security.deployment.SecurityDependencyProcessor;
import org.jboss.as.security.deployment.SecurityEnablementProcessor;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.as.security.service.SecurityBootstrapService;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SubjectFactory;
import org.jboss.security.auth.callback.JBossCallbackHandler;
import org.jboss.security.auth.login.XMLLoginConfigImpl;
import org.jboss.security.authentication.JBossCachedAuthenticationManager;
import org.jboss.security.plugins.JBossAuthorizationManager;
import org.jboss.security.plugins.JBossSecuritySubjectFactory;
import org.jboss.security.plugins.audit.JBossAuditManager;
import org.jboss.security.plugins.identitytrust.JBossIdentityTrustManager;
import org.jboss.security.plugins.mapping.JBossMappingManager;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Jason T. Greene
 */
public class SecuritySubsystemRootResourceDefinition extends SimpleResourceDefinition {

    private static final RuntimeCapability<Void> SECURITY_SUBSYSTEM = RuntimeCapability.Builder.of("org.wildfly.legacy-security").build();
    private static final RuntimeCapability<Void> SERVER_SECURITY_MANAGER = RuntimeCapability.Builder.of("org.wildfly.legacy-security.server-security-manager")
            .setServiceType(ServerSecurityManager.class)
            .build();
    private static final RuntimeCapability<Void> SUBJECT_FACTORY_CAP = RuntimeCapability.Builder.of("org.wildfly.legacy-security.subject-factory")
            .setServiceType(SubjectFactory.class)
            .build();

    private static final SensitiveTargetAccessConstraintDefinition MISC_SECURITY_SENSITIVITY = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(SecurityExtension.SUBSYSTEM_NAME, "misc-security", false, true, true));

    static final SecuritySubsystemRootResourceDefinition INSTANCE = new SecuritySubsystemRootResourceDefinition();

    static final SimpleAttributeDefinition DEEP_COPY_SUBJECT_MODE = new SimpleAttributeDefinitionBuilder(Constants.DEEP_COPY_SUBJECT_MODE, ModelType.BOOLEAN, true)
                    .setAccessConstraints(MISC_SECURITY_SENSITIVITY)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();
    static final SimpleAttributeDefinition INITIALIZE_JACC = new SimpleAttributeDefinitionBuilder(Constants.INITIALIZE_JACC, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .build();

    private SecuritySubsystemRootResourceDefinition() {
        super(new Parameters(SecurityExtension.PATH_SUBSYSTEM,
                SecurityExtension.getResourceDescriptionResolver(SecurityExtension.SUBSYSTEM_NAME))
                .setAddHandler(SecuritySubsystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(SECURITY_SUBSYSTEM, SERVER_SECURITY_MANAGER, SUBJECT_FACTORY_CAP));
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
         resourceRegistration.registerReadWriteAttribute(DEEP_COPY_SUBJECT_MODE, null, new ReloadRequiredWriteAttributeHandler(DEEP_COPY_SUBJECT_MODE));
        resourceRegistration.registerReadWriteAttribute(INITIALIZE_JACC, null, new ReloadRequiredWriteAttributeHandler(INITIALIZE_JACC));
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(SECURITY_SUBSYSTEM);
    }

    private static class SecuritySubsystemAdd extends AbstractBoottimeAddStepHandler {
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

        public static final OperationStepHandler INSTANCE = new SecuritySubsystemAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            DEEP_COPY_SUBJECT_MODE.validateAndSet(operation, model);
            INITIALIZE_JACC.validateAndSet(operation, model);
        }

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            SecurityLogger.ROOT_LOGGER.activatingSecuritySubsystem();

            if(context.getProcessType() != ProcessType.APPLICATION_CLIENT) {
                //remove once AS7-4687 is resolved
                WildFlySecurityManager.setPropertyPrivileged(SecurityContextAssociation.SECURITYCONTEXT_THREADLOCAL, "true");
            }
            final ServiceTarget target = context.getServiceTarget();
            ModelNode initializeJaccNode = SecuritySubsystemRootResourceDefinition.INITIALIZE_JACC.resolveModelAttribute(context,model);
            final SecurityBootstrapService bootstrapService = new SecurityBootstrapService(initializeJaccNode.asBoolean());
            target.addService(SecurityBootstrapService.SERVICE_NAME, bootstrapService)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, bootstrapService.getServiceModuleLoaderInjectedValue())
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

            // add service to bind SecurityDomainJndiInjectable to JNDI
            final SecurityDomainJndiInjectable securityDomainJndiInjectable = new SecurityDomainJndiInjectable();
            final BinderService binderService = new BinderService("jaas");
            target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("jaas"), binderService)
                .addInjection(binderService.getManagedObjectInjector(), securityDomainJndiInjectable)
                .addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class, securityDomainJndiInjectable.getSecurityManagementInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

            // add security management service
            ModelNode modelNode = SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE.resolveModelAttribute(context,model);
            final SecurityManagementService securityManagementService = new SecurityManagementService(
                AUTHENTICATION_MANAGER, modelNode.isDefined() && modelNode.asBoolean(), CALLBACK_HANDLER,
                AUTHORIZATION_MANAGER, AUDIT_MANAGER, IDENTITY_TRUST_MANAGER, MAPPING_MANAGER);
            target.addService(SecurityManagementService.SERVICE_NAME, securityManagementService)
                    .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, securityManagementService.getServiceModuleLoaderInjectedValue())
                    .addDependency(JaasConfigurationService.SERVICE_NAME) // We need to ensure the global JAAS Configuration has been set.
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();

            // add subject factory service
            final SubjectFactoryService subjectFactoryService = new SubjectFactoryService(SUBJECT_FACTORY);
            target.addService(SUBJECT_FACTORY_CAP.getCapabilityServiceName(), subjectFactoryService)
                .addAliases(SubjectFactoryService.SERVICE_NAME)
                .addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class,
                        subjectFactoryService.getSecurityManagementInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

            // add jaas configuration service
            Configuration loginConfig = XMLLoginConfigImpl.getInstance();
            final JaasConfigurationService jaasConfigurationService = new JaasConfigurationService(loginConfig);
            target.addService(JaasConfigurationService.SERVICE_NAME, jaasConfigurationService)
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

            //setup the transaction manager locator

            if(context.hasOptionalCapability("org.wildfly.transactions", SECURITY_SUBSYSTEM.getName(), null)) {
                TransactionManagerLocatorService service = new TransactionManagerLocatorService();
                target.addService(TransactionManagerLocatorService.SERVICE_NAME, service)
                        .addDependency( ServiceName.JBOSS.append("txn", "TransactionManager"), TransactionManager.class, service.getTransactionManagerInjectedValue())
                .install();
            } else {
                target.addService(TransactionManagerLocatorService.SERVICE_NAME, Service.NULL).install();
            }

            //add Simple Security Manager Service
            final SimpleSecurityManagerService simpleSecurityManagerService = new SimpleSecurityManagerService();

            target.addService(SERVER_SECURITY_MANAGER.getCapabilityServiceName(), simpleSecurityManagerService)
                .addAliases(SimpleSecurityManagerService.SERVICE_NAME)
                .addDependency(SecurityManagementService.SERVICE_NAME)
                .install();

            context.addStep(new AbstractDeploymentChainStep() {
                @Override
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(SecurityExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JACC_POLICY,
                            new JaccEarDeploymentProcessor());
                    processorTarget.addDeploymentProcessor(SecurityExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_SECURITY,
                            new SecurityDependencyProcessor());
                    processorTarget.addDeploymentProcessor(SecurityExtension.SUBSYSTEM_NAME, Phase.PARSE, 0x0080,
                            new SecurityEnablementProcessor());

                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}
