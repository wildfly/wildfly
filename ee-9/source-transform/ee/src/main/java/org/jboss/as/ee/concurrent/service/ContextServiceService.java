/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.concurrent.service;

import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.jboss.as.ee.concurrent.ContextServiceImpl;
import org.jboss.as.ee.concurrent.TransactionSetupProviderImpl;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for managing a context service impl's lifecycle.
 *
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ContextServiceService extends EEConcurrentAbstractService<ContextServiceImpl> {

    private final String name;
    private final ContextSetupProvider contextSetupProvider;
    private final boolean useTransactionSetupProvider;
    private volatile ContextServiceImpl contextService;

    public ContextServiceService(final String name, final String jndiName, final ContextSetupProvider contextSetupProvider, boolean useTransactionSetupProvider) {
        super(jndiName);
        this.name = name;
        this.contextSetupProvider = contextSetupProvider;
        this.useTransactionSetupProvider = useTransactionSetupProvider;
    }

    @Override
    void startValue(final StartContext context) {
        contextService = new ContextServiceImpl(name, contextSetupProvider, (useTransactionSetupProvider ? new TransactionSetupProviderImpl() : null));
    }

    @Override
    void stopValue(final StopContext context) {
        contextService = null;
    }

    public ContextServiceImpl getValue() throws IllegalStateException {
        final ContextServiceImpl value = this.contextService;
        if (value == null) {
            throw EeLogger.ROOT_LOGGER.concurrentServiceValueUninitialized();
        }
        return value;
    }

}
