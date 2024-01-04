/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.ServerAuthenticationContext;

/**
 * A {@link ResourceDefinition} to define the security domains to attempt to outflow an established identity to.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class IdentityResourceDefinition extends SimpleResourceDefinition {

    private static final String SECURITY_DOMAIN_CAPABILITY_NAME = "org.wildfly.security.security-domain";

    public static final String IDENTITY_CAPABILITY_NAME = "org.wildfly.ejb3.identity";
    static final RuntimeCapability<Void> IDENTITY_CAPABILITY = RuntimeCapability.Builder.of(IDENTITY_CAPABILITY_NAME, Function.class)
            .build();

    public static final StringListAttributeDefinition OUTFLOW_SECURITY_DOMAINS = new StringListAttributeDefinition.Builder(EJB3SubsystemModel.OUTFLOW_SECURITY_DOMAINS)
            .setRequired(false)
            .setMinSize(1)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setCapabilityReference(SECURITY_DOMAIN_CAPABILITY_NAME, IDENTITY_CAPABILITY)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.ELYTRON_SECURITY_DOMAIN_REF)
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { OUTFLOW_SECURITY_DOMAINS };

    IdentityResourceDefinition(List<String> outflowSecurityDomains) {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.IDENTITY_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.IDENTITY))
                .setAddHandler(new AddHandler(outflowSecurityDomains))
                // .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setCapabilities(IDENTITY_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute,  null, handler);
        }
    }

    private static class AddHandler extends AbstractAddStepHandler {
        private final List<String> outflowSecurityDomains;

        private AddHandler(List<String> outflowSecurityDomains) {
            super(OUTFLOW_SECURITY_DOMAINS);
            this.outflowSecurityDomains = outflowSecurityDomains;
        }

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.populateModel(context, operation, resource);
            this.outflowSecurityDomains.clear();
            this.outflowSecurityDomains.addAll(OUTFLOW_SECURITY_DOMAINS.unwrap(context, resource.getModel()));
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final List<Supplier<SecurityDomain>> outflowSecurityDomainSuppliers = new ArrayList<>();
            final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(IDENTITY_CAPABILITY);
            final Consumer<Function<SecurityIdentity, Set<SecurityIdentity>>> consumer = sb.provides(IDENTITY_CAPABILITY);
            for (String outflowSecurityDomain : outflowSecurityDomains) {
                outflowSecurityDomainSuppliers.add(sb.requiresCapability(SECURITY_DOMAIN_CAPABILITY_NAME, SecurityDomain.class, outflowSecurityDomain));
            }
            IdentityService identityService = new IdentityService(consumer, outflowSecurityDomainSuppliers);
            sb.setInstance(identityService).install();
        }
    }

    static class IdentityService implements Service {
        private final Consumer<Function<SecurityIdentity, Set<SecurityIdentity>>> consumer;
        private final List<Supplier<SecurityDomain>> outflowSecurityDomainSuppliers;
        private Set<SecurityDomain> outflowSecurityDomains = new HashSet<>();

        private IdentityService(final Consumer<Function<SecurityIdentity, Set<SecurityIdentity>>> consumer, final List<Supplier<SecurityDomain>> outflowSecurityDomainSuppliers) {
            this.consumer = consumer;
            this.outflowSecurityDomainSuppliers = outflowSecurityDomainSuppliers;
        }

        @Override
        public void start(StartContext context) throws StartException {
            HashSet<SecurityDomain> securityDomains = new HashSet<>();
            for (Supplier<SecurityDomain> outflowSecurityDomainInjector : outflowSecurityDomainSuppliers) {
                SecurityDomain value = outflowSecurityDomainInjector.get();
                securityDomains.add(value);
            }
            outflowSecurityDomains.addAll(securityDomains);
            consumer.accept(this::outflowIdentity);
        }

        private Set<SecurityIdentity> outflowIdentity(final SecurityIdentity securityIdentity) {
            Set<SecurityIdentity> outflowedIdentities = new HashSet<>(outflowSecurityDomains.size());
            if (securityIdentity != null) {
                // Attempt to outflow the established identity to each domain in the list
                for (SecurityDomain outflowSecurityDomain : outflowSecurityDomains) {
                    try {
                        ServerAuthenticationContext serverAuthenticationContext = outflowSecurityDomain.createNewAuthenticationContext();
                        if (serverAuthenticationContext.importIdentity(securityIdentity)) {
                            outflowedIdentities.add(serverAuthenticationContext.getAuthorizedIdentity());
                        }
                    } catch (RealmUnavailableException | IllegalStateException e) {
                        // Ignored
                    }
                }
            }
            return outflowedIdentities;
        }

        @Override
        public void stop(StopContext context) {
            consumer.accept(null);
            outflowSecurityDomains.clear();
        }
    }
}
