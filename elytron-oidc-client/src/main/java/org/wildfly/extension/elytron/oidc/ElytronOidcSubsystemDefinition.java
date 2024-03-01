/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.server.security.VirtualDomainUtil.OIDC_VIRTUAL_SECURITY_DOMAIN_CREATION_SERVICE;
import static org.jboss.as.server.security.VirtualDomainUtil.VIRTUAL_SECURITY_DOMAIN_CREATION_SERVICE;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.security.VirtualSecurityDomainCreationService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;

/**
 * Root subsystem definition for the Elytron OpenID Connect subsystem.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class ElytronOidcSubsystemDefinition extends PersistentResourceDefinition {
    static final PathElement PATH = SubsystemResourceDefinitionRegistrar.pathElement(ElytronOidcExtension.SUBSYSTEM_NAME);
    static final String CONFIG_CAPABILITY_NAME = "org.wildlfly.elytron.oidc";
    static final String ELYTRON_CAPABILITY_NAME = "org.wildfly.security.elytron";

    static final RuntimeCapability<Void> CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(CONFIG_CAPABILITY_NAME)
                    .setServiceType(Void.class)
                    .addRequirements(ELYTRON_CAPABILITY_NAME)
                    .build();

    protected ElytronOidcSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(ElytronOidcExtension.SUBSYSTEM_PATH,
                ElytronOidcExtension.getResourceDescriptionResolver())
                .setAddHandler(new ElytronOidcSubsystemAdd())
                .setRemoveHandler(ElytronOidcSubsystemRemove.INSTANCE)
                .setCapabilities(CONFIG_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new RealmDefinition());
        resourceRegistration.registerSubModel(new ProviderDefinition());
        resourceRegistration.registerSubModel(new SecureDeploymentDefinition());
        resourceRegistration.registerSubModel(new SecureServerDefinition());
    }

    private static class ElytronOidcSubsystemRemove extends ReloadRequiredRemoveStepHandler {

        static final ElytronOidcSubsystemRemove INSTANCE = new ElytronOidcSubsystemRemove();

        private ElytronOidcSubsystemRemove() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            if (context.isResourceServiceRestartAllowed()) {
                context.removeService(OIDC_VIRTUAL_SECURITY_DOMAIN_CREATION_SERVICE);
            } else {
                context.reloadRequired();
            }
        }

        @Override
        protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            ServiceTarget target = context.getCapabilityServiceTarget();
            installService(VIRTUAL_SECURITY_DOMAIN_CREATION_SERVICE, new VirtualSecurityDomainCreationService(), target);
        }
    }

    static void installService(ServiceName serviceName, Service<?> service, ServiceTarget serviceTarget) {
        serviceTarget.addService(serviceName, service)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
