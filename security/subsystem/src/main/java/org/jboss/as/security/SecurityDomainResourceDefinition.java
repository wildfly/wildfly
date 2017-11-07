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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.security.Principal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.security.CacheableManager;
import org.jboss.security.SimplePrincipal;

/**
 * @author Jason T. Greene
 */
class SecurityDomainResourceDefinition extends SimpleResourceDefinition {

    static final String CACHE_CONTAINER_NAME = "security";
    static final String INFINISPAN_CACHE_TYPE = "infinispan";
    static final RuntimeCapability<Void> LEGACY_SECURITY_DOMAIN = RuntimeCapability.Builder.of("org.wildfly.security.legacy-security-domain", true)
            .setServiceType(SecurityDomainContext.class)
            .build();

    public static final SimpleAttributeDefinition CACHE_TYPE = new SimpleAttributeDefinitionBuilder(Constants.CACHE_TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new StringAllowedValuesValidator("default", INFINISPAN_CACHE_TYPE))
            .build();

    private final boolean registerRuntimeOnly;
    private final List<AccessConstraintDefinition> accessConstraints;

    SecurityDomainResourceDefinition(boolean registerRuntimeOnly) {
        super(new Parameters(SecurityExtension.SECURITY_DOMAIN_PATH,
                SecurityExtension.getResourceDescriptionResolver(Constants.SECURITY_DOMAIN))
                .setAddHandler(SecurityDomainAdd.INSTANCE)
                .setRemoveHandler(new ServiceRemoveStepHandler(SecurityDomainService.SERVICE_NAME, SecurityDomainAdd.INSTANCE) {
                    @Override
                    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
                        super.performRuntime(context, operation, model);
                        if (context.isResourceServiceRestartAllowed()) {
                            String cacheType = SecurityDomainAdd.getAuthenticationCacheType(model);
                            String securityDomain = context.getCurrentAddressValue();
                            if (SecurityDomainResourceDefinition.INFINISPAN_CACHE_TYPE.equals(cacheType)) {
                                for (org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement requirement : EnumSet.allOf(org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement.class)) {
                                    context.removeService(requirement.getServiceName(context, CACHE_CONTAINER_NAME, securityDomain));
                                }
                            }
                        }
                    }
                })
                .setCapabilities(LEGACY_SECURITY_DOMAIN));
        this.registerRuntimeOnly = registerRuntimeOnly;
        ApplicationTypeConfig atc = new ApplicationTypeConfig(SecurityExtension.SUBSYSTEM_NAME, Constants.SECURITY_DOMAIN);
        AccessConstraintDefinition acd = new ApplicationTypeAccessConstraintDefinition(atc);
        this.accessConstraints = Arrays.asList(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN, acd);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(CACHE_TYPE, null, new SecurityDomainReloadWriteHandler(CACHE_TYPE));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        if (registerRuntimeOnly) {
            resourceRegistration.registerOperationHandler(ListCachePrincipals.DEFINITION, ListCachePrincipals.INSTANCE);
            resourceRegistration.registerOperationHandler(FlushOperation.DEFINITION,FlushOperation.INSTANCE);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    public static ServiceName getSecurityDomainServiceName(PathAddress pathAddress) {
        PathAddress domain = Util.getParentAddressByKey(pathAddress, Constants.SECURITY_DOMAIN);
        if (domain == null)
            throw SecurityLogger.ROOT_LOGGER.addressDidNotContainSecurityDomain();
        return SecurityDomainService.SERVICE_NAME.append(domain.getLastElement().getValue());
   }

    @SuppressWarnings("unchecked")
    private static ServiceController<SecurityDomainContext> getSecurityDomainService(OperationContext context, String securityDomain) {
        return (ServiceController<SecurityDomainContext>) context
                .getServiceRegistry(false)
                .getRequiredService(SecurityDomainService.SERVICE_NAME.append(securityDomain));
    }

    static class ListCachePrincipals extends AbstractRuntimeOnlyHandler {
        static final ListCachePrincipals INSTANCE = new ListCachePrincipals();
        static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(Constants.LIST_CACHED_PRINCIPALS,
                SecurityExtension.getResourceDescriptionResolver(Constants.LIST_CACHED_PRINCIPALS))
                .setReadOnly()
                .setRuntimeOnly()
                .setReplyType(ModelType.LIST)
                .setReplyValueType(ModelType.STRING)
                .build();


        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode opAddr = operation.require(OP_ADDR);
            PathAddress address = PathAddress.pathAddress(opAddr);
            final String securityDomain = address.getLastElement().getValue();

            ServiceController<SecurityDomainContext> controller = getSecurityDomainService(context, securityDomain);
            if (controller != null) {
                // FIXME this is nasty.
                waitForService(controller);
                SecurityDomainContext sdc = controller.getValue();
                @SuppressWarnings("unchecked")
                CacheableManager<?, Principal> manager = (CacheableManager<?, Principal>) sdc
                        .getAuthenticationManager();
                Set<Principal> cachedPrincipals = manager.getCachedKeys();
                ModelNode result = context.getResult();
                for (Principal principal : cachedPrincipals) {
                    result.add(principal.getName());
                }
                if (!result.isDefined())
                    result.setEmptyList();
            } else {
                throw SecurityLogger.ROOT_LOGGER.noAuthenticationCacheAvailable(securityDomain);
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    static final class FlushOperation extends AbstractRuntimeOnlyHandler {
        static final FlushOperation INSTANCE = new FlushOperation();
        static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(Constants.FLUSH_CACHE,
                SecurityExtension.getResourceDescriptionResolver(Constants.SECURITY_DOMAIN))
                .setEntryType(OperationEntry.EntryType.PUBLIC)
                .setRuntimeOnly()
                .addParameter(new SimpleAttributeDefinitionBuilder(Constants.PRINCIPAL_ARGUMENT, ModelType.STRING).setRequired(false).build())
                .build();

        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode opAddr = operation.require(OP_ADDR);
            PathAddress address = PathAddress.pathAddress(opAddr);
            final String securityDomain = address.getLastElement().getValue();
            String principal = null;
            if (operation.hasDefined(Constants.PRINCIPAL_ARGUMENT))
                principal = operation.get(Constants.PRINCIPAL_ARGUMENT).asString();

            ServiceController<SecurityDomainContext> controller = getSecurityDomainService(context, securityDomain);
            if (controller != null) {
                // FIXME this is nasty.
                waitForService(controller);
                SecurityDomainContext sdc = controller.getValue();
                @SuppressWarnings("unchecked")
                CacheableManager<?, Principal> manager = (CacheableManager<?, Principal>) sdc.getAuthenticationManager();
                if (principal != null)
                    manager.flushCache(new SimplePrincipal(principal));
                else
                    manager.flushCache();
            } else {
                throw SecurityLogger.ROOT_LOGGER.noAuthenticationCacheAvailable(securityDomain);
            }
            // Can't rollback
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    /**
     * Wait for the required service to start up and fail otherwise. This method is necessary when a runtime operation
     * uses a service that might have been created within a composite operation.
     *
     * This method will wait at most 100 millis.
     *
     * @param controller the service to wait for
     * @throws OperationFailedException if the service is not available, or the thread was interrupted.
     */
    private static void waitForService(final ServiceController<?> controller) throws OperationFailedException {
        if (controller.getState() == ServiceController.State.UP) return;

        final StabilityMonitor monitor = new StabilityMonitor();
        monitor.addController(controller);
        try {
            monitor.awaitStability(100, MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SecurityLogger.ROOT_LOGGER.interruptedWaitingForSecurityDomain(controller.getName().getSimpleName());
        } finally {
            monitor.removeController(controller);
        }

        if (controller.getState() != ServiceController.State.UP) {
            throw SecurityLogger.ROOT_LOGGER.requiredSecurityDomainServiceNotAvailable(controller.getName().getSimpleName());
        }
    }

}
