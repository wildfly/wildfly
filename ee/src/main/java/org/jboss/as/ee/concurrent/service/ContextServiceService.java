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

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;
import org.jboss.as.ee.EeMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for managing a context service impl's lifecycle.
 *
 * @author Eduardo Martins
 */
public class ContextServiceService implements Service<ContextServiceImpl> {

    public static final Class<?> SERVICE_VALUE_TYPE = ContextServiceImpl.class;

    private final String name;
    private final ContextSetupProvider contextSetupProvider;
    private final TransactionSetupProvider transactionSetupProvider;

    private ContextServiceImpl contextService;

    /**
     * @param name
     * @param contextSetupProvider
     * @param transactionSetupProvider
     * @see ContextServiceImpl#ContextServiceImpl(String, org.glassfish.enterprise.concurrent.spi.ContextSetupProvider, org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider)
     */
    public ContextServiceService(String name, ContextSetupProvider contextSetupProvider, TransactionSetupProvider transactionSetupProvider) {
        this.name = name;
        this.contextSetupProvider = contextSetupProvider;
        this.transactionSetupProvider = transactionSetupProvider;
    }

    public void start(final StartContext context) throws StartException {
        contextService = new ContextServiceImpl(name, contextSetupProvider, transactionSetupProvider);
    }

    public void stop(final StopContext context) {
        contextService = null;
    }

    public ContextServiceImpl getValue() throws IllegalStateException {
        final ContextServiceImpl value = this.contextService;
        if (value == null) {
            throw EeMessages.MESSAGES.concurrentServiceValueUninitialized();
        }
        return value;
    }

}
