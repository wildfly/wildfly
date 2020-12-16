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
package org.wildfly.extension.rts.jaxrs;

import java.io.IOException;
import javax.annotation.Priority;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.wildfly.extension.rts.logging.RTSLogger;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.LocalTransactionContext;


/**
 * <p>
 *   Request and response filter which is expected to be called
 *   for the request side after the {@link org.jboss.narayana.rest.bridge.inbound.InboundBridgeFilter} is processed
 *   while on the response side before the {@link org.jboss.narayana.rest.bridge.inbound.InboundBridgeFilter} is processed.
 * </p>
 * <p>
 *   Purpose of this filter is an integration of WFTC with Narayana REST-AT Inbound Bridge.
 *   Inbound Bridge uses Narayana transactions while WFLY utilizes WFTC transactions (wrapping the underlaying Narayana) ones.
 *   For that on request side the Inbound bridge first creates the Narayana transaction and then the request
 *   filter makes the WFTC to wrap and to know about this transaction.
 *   On the response side the WFTC needs to suspend its wrapping transaction and then Narayana suspend its own
 *   transaction (which was already suspended by WFTC callback and thus is ignored on the Narayana side).
 * </p>
 * <p>
 *   The {@link org.jboss.narayana.rest.bridge.inbound.InboundBridgeFilter} defines its {@link Priority}
 *   as <code>{@code Priorities#USER}-100</code>. This WildFly filter has to be processed after the Narayana
 *   one and it needs to define higher priority.
 * </p>
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@Provider
@Priority(Priorities.USER - 80)
public class ImportWildflyClientGlobalTransactionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            // pull in any RTS transaction
            LocalTransactionContext.getCurrent().importProviderTransaction();
        } catch (SystemException se) {
            throw RTSLogger.ROOT_LOGGER.failueOnImportingGlobalTransactionFromWildflyClient(se);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        try {
            // suspending context before returning to the client
            if (ContextTransactionManager.getInstance() != null &&
                    ContextTransactionManager.getInstance().getStatus() != Status.STATUS_NO_TRANSACTION) {
                ContextTransactionManager.getInstance().suspend();
            }
        } catch (SystemException se) {
            RTSLogger.ROOT_LOGGER.cannotGetTransactionStatus(responseContext, se);
        }
    }
}
