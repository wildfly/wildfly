/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
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
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
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
            .setCapabilityReference(SECURITY_DOMAIN_CAPABILITY_NAME, IDENTITY_CAPABILITY_NAME, false)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.ELYTRON_SECURITY_DOMAIN_REF)
            .build();

    static final IdentityResourceDefinition INSTANCE = new IdentityResourceDefinition();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { OUTFLOW_SECURITY_DOMAINS };

    private static List<String> outflowSecurityDomains = Collections.synchronizedList(new ArrayList<>());

    private IdentityResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.IDENTITY_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.IDENTITY))
                .setAddHandler(new AddHandler())
                // .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveHandler(new ReloadRequiredRemoveStepHandler(IDENTITY_CAPABILITY))
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setCapabilities(IDENTITY_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        outflowSecurityDomains.clear();
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute,  null, handler);
        }
    }

    private static class AddHandler extends AbstractAddStepHandler {

        private AddHandler() {
            super(IDENTITY_CAPABILITY, OUTFLOW_SECURITY_DOMAINS);
        }

        @Override
        protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.populateModel(context, operation, resource);
            outflowSecurityDomains = OUTFLOW_SECURITY_DOMAINS.unwrap(context, resource.getModel());
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            IdentityService identityService = new IdentityService();

            CapabilityServiceBuilder<Function<SecurityIdentity, Set<SecurityIdentity>>> capabilityServiceBuilder = context.getCapabilityServiceTarget().addCapability(IDENTITY_CAPABILITY, identityService);
            for (String outflowSecurityDomain : outflowSecurityDomains) {
                capabilityServiceBuilder.addCapabilityRequirement(SECURITY_DOMAIN_CAPABILITY_NAME, SecurityDomain.class, identityService.createOutflowSecurityDomainInjector(), outflowSecurityDomain);
            }
            capabilityServiceBuilder.setInitialMode(Mode.ACTIVE).install();
        }
    }

    static class IdentityService implements Service<Function<SecurityIdentity, Set<SecurityIdentity>>> {

        private final List<InjectedValue<SecurityDomain>> outflowSecurityDomainInjectors = new ArrayList<>();
        private Set<SecurityDomain> outflowSecurityDomains = new HashSet<>();

        @Override
        public void start(StartContext context) throws StartException {
            HashSet<SecurityDomain> securityDomains = new HashSet<>();
            for (InjectedValue<SecurityDomain> outflowSecurityDomainInjector : outflowSecurityDomainInjectors) {
                SecurityDomain value = outflowSecurityDomainInjector.getValue();
                securityDomains.add(value);
            }
            outflowSecurityDomains.addAll(securityDomains);
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
            outflowSecurityDomains = null;
        }

        @Override
        public Function<SecurityIdentity, Set<SecurityIdentity>> getValue() throws IllegalStateException, IllegalArgumentException {
            return this::outflowIdentity;
        }

        Injector<SecurityDomain> createOutflowSecurityDomainInjector() {
            InjectedValue<SecurityDomain> injectedValue = new InjectedValue<>();
            outflowSecurityDomainInjectors.add(injectedValue);
            return injectedValue;
        }

    }

    BooleanSupplier getOutflowSecurityDomainsConfiguredSupplier() {
        return () -> ! outflowSecurityDomains.isEmpty();
    }
}
