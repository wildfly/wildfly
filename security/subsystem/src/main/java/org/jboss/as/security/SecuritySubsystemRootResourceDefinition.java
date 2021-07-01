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

import java.security.Policy;

import javax.security.auth.login.Configuration;
import javax.transaction.TransactionManager;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
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
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.security.context.SecurityDomainJndiInjectable;
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
import org.jboss.msc.service.ServiceBuilder;
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
    private static final RuntimeCapability<Void> JACC_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.legacy-security.jacc")
            .setServiceType(Policy.class)
            .build();
    private static final RuntimeCapability<Void> JACC_CAPABILITY_TOMBSTONE = RuntimeCapability.Builder.of("org.wildfly.legacy-security.jacc.tombstone")
       .setServiceType(Void.class)
       .build();

    private static final SensitiveTargetAccessConstraintDefinition MISC_SECURITY_SENSITIVITY = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(SecurityExtension.SUBSYSTEM_NAME, "misc-security", false, true, true));

    static final SecuritySubsystemRootResourceDefinition INSTANCE = new SecuritySubsystemRootResourceDefinition();

    static final SimpleAttributeDefinition DEEP_COPY_SUBJECT_MODE = new SimpleAttributeDefinitionBuilder(Constants.DEEP_COPY_SUBJECT_MODE, ModelType.BOOLEAN, true)
                    .setAccessConstraints(MISC_SECURITY_SENSITIVITY)
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .build();
    static final SimpleAttributeDefinition INITIALIZE_JACC = new SimpleAttributeDefinitionBuilder(Constants.INITIALIZE_JACC, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.TRUE)
            .setRestartJVM()
            .setAllowExpression(true)
            .build();

    private SecuritySubsystemRootResourceDefinition() {
        super(new Parameters(SecurityExtension.PATH_SUBSYSTEM,
                SecurityExtension.getResourceDescriptionResolver(SecurityExtension.SUBSYSTEM_NAME))
                .setAddHandler(SecuritySubsystemAdd.INSTANCE)
                .setRemoveHandler(new ReloadRequiredRemoveStepHandler() {

                    @Override
                    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation,
                            Resource resource) throws OperationFailedException {
                        super.recordCapabilitiesAndRequirements(context, operation, resource);
                        context.deregisterCapability(JACC_CAPABILITY.getName());
                    }
                })
                .setCapabilities(SECURITY_SUBSYSTEM, SERVER_SECURITY_MANAGER, SUBJECT_FACTORY_CAP));
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
         resourceRegistration.registerReadWriteAttribute(DEEP_COPY_SUBJECT_MODE, null, new ReloadRequiredWriteAttributeHandler(DEEP_COPY_SUBJECT_MODE));
        resourceRegistration.registerReadWriteAttribute(INITIALIZE_JACC, null, new ReloadRequiredWriteAttributeHandler(INITIALIZE_JACC) {
            @Override
            protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandback) throws OperationFailedException {
                // As the PolicyConfigurationFactory is a singleton, once it's initialized any changes will require a restart
                CapabilityServiceSupport capabilitySupport = context.getCapabilityServiceSupport();
                final boolean elytronJacc = capabilitySupport.hasCapability("org.wildfly.security.jacc-policy");
                if (resolvedValue.asBoolean() && elytronJacc) {
                    throw SecurityLogger.ROOT_LOGGER.unableToEnableJaccSupport();
                }
                return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, voidHandback);
            }

             @Override
            protected void recordCapabilitiesAndRequirements(OperationContext context,
                                                             AttributeDefinition attributeDefinition,
                                                             ModelNode newValue,
                                                             ModelNode oldValue) {
                super.recordCapabilitiesAndRequirements(context, attributeDefinition, newValue, oldValue);

                boolean shouldRegister = resolveValue(context, attributeDefinition, newValue);
                boolean registered = resolveValue(context, attributeDefinition, oldValue);

                if (!shouldRegister) {
                    context.deregisterCapability(JACC_CAPABILITY.getName());
                }
                if (!registered && shouldRegister) {
                    context.registerCapability(JACC_CAPABILITY);
                    // do not register the JACC_CAPABILITY_TOMBSTONE at this point - it will be registered on restart
                }
            }

            private boolean resolveValue(OperationContext context, AttributeDefinition attributeDefinition, ModelNode node) {
                try {
                    return attributeDefinition.resolveValue(context, node).asBoolean();
                } catch (OperationFailedException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
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
        protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource)
                throws OperationFailedException {
            super.recordCapabilitiesAndRequirements(context, operation, resource);
            if (INITIALIZE_JACC.resolveModelAttribute(context, resource.getModel()).asBoolean()) {
                context.registerCapability(JACC_CAPABILITY);
                // tombstone marks the Policy being initiated and should not be removed until restart
                if (context.isBooting()) {
                    context.registerCapability(JACC_CAPABILITY_TOMBSTONE);
                }
            }
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
            ServiceBuilder<Void> builder = target.addService(SecurityBootstrapService.SERVICE_NAME, bootstrapService)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, bootstrapService.getServiceModuleLoaderInjectedValue())
                .setInitialMode(ServiceController.Mode.ACTIVE);
            if (initializeJaccNode.asBoolean()) {
                builder.addAliases(context.getCapabilityServiceName(JACC_CAPABILITY.getName(), Policy.class));
            }
            builder.install();

            // add service to bind SecurityDomainJndiInjectable to JNDI
            final SecurityDomainJndiInjectable securityDomainJndiInjectable = new SecurityDomainJndiInjectable();
            final BinderService binderService = new BinderService("jaas");
            binderService.getManagedObjectInjector().inject(securityDomainJndiInjectable);
            target.addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("jaas"), binderService)
                .addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class, securityDomainJndiInjectable.getSecurityManagementInjector())
                .install();

            // add security management service
            ModelNode modelNode = SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE.resolveModelAttribute(context,model);
            final SecurityManagementService securityManagementService = new SecurityManagementService(
                AUTHENTICATION_MANAGER, modelNode.isDefined() && modelNode.asBoolean(), CALLBACK_HANDLER,
                AUTHORIZATION_MANAGER, AUDIT_MANAGER, IDENTITY_TRUST_MANAGER, MAPPING_MANAGER);
            final ServiceBuilder securityManagementServiceSB = target.addService(SecurityManagementService.SERVICE_NAME, securityManagementService);
            securityManagementServiceSB.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, securityManagementService.getServiceModuleLoaderInjectedValue());
            securityManagementServiceSB.requires(JaasConfigurationService.SERVICE_NAME); // We need to ensure the global JAAS Configuration has been set.
            securityManagementServiceSB.setInitialMode(ServiceController.Mode.ACTIVE).install();

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

            final ServiceBuilder simpleSecurityManagerServiceSB = target.addService(SERVER_SECURITY_MANAGER.getCapabilityServiceName(), simpleSecurityManagerService);
            simpleSecurityManagerServiceSB.addAliases(SimpleSecurityManagerService.SERVICE_NAME);
            simpleSecurityManagerServiceSB.requires(SecurityManagementService.SERVICE_NAME);
            simpleSecurityManagerServiceSB.install();

            context.addStep(new AbstractDeploymentChainStep() {
                @Override
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(SecurityExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_SECURITY,
                            new SecurityDependencyProcessor());
                    processorTarget.addDeploymentProcessor(SecurityExtension.SUBSYSTEM_NAME, Phase.PARSE, 0x0080,
                            new SecurityEnablementProcessor());

                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required("javax.security.auth.message.api"));
    }
}
