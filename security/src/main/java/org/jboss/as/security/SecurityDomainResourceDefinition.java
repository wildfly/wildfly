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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.security.Principal;
import java.util.EnumSet;
import java.util.Set;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.security.CacheableManager;
import org.jboss.security.SimplePrincipal;

/**
 * @author Jason T. Greene
 */
class SecurityDomainResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition CACHE_TYPE = new SimpleAttributeDefinitionBuilder(Constants.CACHE_TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .build();

    private final boolean registerRuntimeOnly;

    SecurityDomainResourceDefinition(boolean registerRuntimeOnly) {
        super(SecurityExtension.SECURITY_DOMAIN_PATH,
                SecurityExtension.getResourceDescriptionResolver(Constants.SECURITY_DOMAIN), SecurityDomainAdd.INSTANCE, SecurityDomainRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

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

    public static ServiceName getSecurityDomainServiceName(PathAddress pathAddress) {
        PathAddress domain = Util.getParentAddressByKey(pathAddress, Constants.SECURITY_DOMAIN);
        if (domain == null)
            throw SecurityMessages.MESSAGES.addressDidNotContainSecurityDomain();
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
                waitFor(controller);
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
                throw SecurityMessages.MESSAGES.noAuthenticationCacheAvailable(securityDomain);
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    static final class FlushOperation extends AbstractRuntimeOnlyHandler {
        static final FlushOperation INSTANCE = new FlushOperation();
        static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinition(Constants.FLUSH_CACHE,
                SecurityExtension.getResourceDescriptionResolver(Constants.SECURITY_DOMAIN),
                OperationEntry.EntryType.PUBLIC, EnumSet.of(Flag.RUNTIME_ONLY),
                new SimpleAttributeDefinition(Constants.PRINCIPAL_ARGUMENT, ModelType.STRING, true)
                );

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
                waitFor(controller);
                SecurityDomainContext sdc = controller.getValue();
                @SuppressWarnings("unchecked")
                CacheableManager<?, Principal> manager = (CacheableManager<?, Principal>) sdc.getAuthenticationManager();
                if (principal != null)
                    manager.flushCache(new SimplePrincipal(principal));
                else
                    manager.flushCache();
            } else {
                throw SecurityMessages.MESSAGES.noAuthenticationCacheAvailable(securityDomain);
            }
            // Can't rollback
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }


}
