/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.rts.jaxrs;

import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
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
