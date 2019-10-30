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
import javax.transaction.SystemException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import org.wildfly.extension.rts.logging.RTSLogger;
import org.wildfly.transaction.client.LocalTransactionContext;


/**
 * Filter which is expected to be called after {@link InboundBridgeFilter} is processed.<br>
 * Inbound bridge manages transactions on Narayana side and this filter causes the transaction
 * from Narayana being imported by Wildfly transaction client.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@Provider
public class ImportWildflyClientGlobalTransactionFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            // pull in any RTS transaction
            LocalTransactionContext.getCurrent().importProviderTransaction();
        } catch (SystemException se) {
            throw RTSLogger.ROOT_LOGGER.failueOnImportingGlobalTransactionFromWildflyClient(se);
        }
    }

}
