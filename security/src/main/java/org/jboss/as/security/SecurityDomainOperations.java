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
import java.util.Set;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.security.CacheableManager;
import org.jboss.security.SimplePrincipal;

/**
 * Group of operations that can be invoked for a security domain.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
class SecurityDomainOperations {

    static final String LIST_CACHED_PRINCIPALS = "list-cached-principals";

    static final String FLUSH_CACHE = "flush-cache";

    private static final String PRINCIPAL_ARGUMENT = "principal";

    static final ModelQueryOperationHandler LIST_CACHED_PRINCIPALS_OP = new ModelQueryOperationHandler() {

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation,
                final ResultHandler resultHandler) throws OperationFailedException {
            ModelNode opAddr = operation.require(OP_ADDR);
            PathAddress address = PathAddress.pathAddress(opAddr);
            final String securityDomain = address.getLastElement().getValue();

            if (context.getRuntimeContext() != null) {
                context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void execute(RuntimeTaskContext context) throws OperationFailedException {
                        ServiceController<SecurityDomainContext> controller = (ServiceController<SecurityDomainContext>) context
                                .getServiceRegistry().getRequiredService(
                                        SecurityDomainService.SERVICE_NAME.append(securityDomain));
                        if (controller != null) {
                            SecurityDomainContext sdc = controller.getValue();
                            CacheableManager<?, Principal> manager = (CacheableManager<?, Principal>) sdc
                                    .getAuthenticationManager();
                            Set<Principal> cachedPrincipals = manager.getCachedKeys();
                            ModelNode result = new ModelNode();
                            for (Principal principal : cachedPrincipals) {
                                result.add(principal.getName());
                            }
                            if (!result.isDefined())
                                result.setEmptyList();
                            resultHandler.handleResultFragment(new String[0], result);
                            resultHandler.handleResultComplete();
                        } else {
                            resultHandler.handleResultFragment(
                                    new String[0],
                                    new ModelNode().set("authentication cache for security domain " + securityDomain
                                            + " available"));
                            resultHandler.handleResultComplete();
                        }
                    }
                });
            } else {
                resultHandler.handleResultFragment(new String[0],
                        new ModelNode().set("authentication cache for security domain " + securityDomain + " available"));
                resultHandler.handleResultComplete();
            }
            return new BasicOperationResult();
        }
    };

    static final ModelQueryOperationHandler FLUSH_CACHE_OP = new ModelQueryOperationHandler() {

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation,
                final ResultHandler resultHandler) throws OperationFailedException {
            ModelNode opAddr = operation.require(OP_ADDR);
            PathAddress address = PathAddress.pathAddress(opAddr);
            final String securityDomain = address.getLastElement().getValue();
            String principal = null;
            if (operation.hasDefined(PRINCIPAL_ARGUMENT))
                principal = operation.get(PRINCIPAL_ARGUMENT).asString();
            final String principalName = principal;

            if (context.getRuntimeContext() != null) {
                context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public void execute(RuntimeTaskContext context) throws OperationFailedException {
                        ServiceController<SecurityDomainContext> controller = (ServiceController<SecurityDomainContext>) context
                                .getServiceRegistry().getRequiredService(
                                        SecurityDomainService.SERVICE_NAME.append(securityDomain));
                        if (controller != null) {
                            SecurityDomainContext sdc = controller.getValue();
                            CacheableManager<?, Principal> manager = (CacheableManager<?, Principal>) sdc
                                    .getAuthenticationManager();
                            if (principalName != null)
                                manager.flushCache(new SimplePrincipal(principalName));
                            else
                                manager.flushCache();
                            resultHandler.handleResultComplete();
                        } else {
                            resultHandler.handleResultFragment(
                                    new String[0],
                                    new ModelNode().set("authentication cache for security domain " + securityDomain
                                            + " available"));
                            resultHandler.handleResultComplete();
                        }
                    }
                });
            } else {
                resultHandler.handleResultFragment(new String[0],
                        new ModelNode().set("authentication cache for security domain " + securityDomain + " available"));
                resultHandler.handleResultComplete();
            }
            return new BasicOperationResult();
        }
    };

}
