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

package org.jboss.as.txn.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.usertx.UserTransactionRegistry;

/**
 * Service responsible for exposing a {@link UserTransactionRegistry} instance.
 *
 * @author John Bailey
 */
public class UserTransactionRegistryService implements Service<UserTransactionRegistry> {

    public static final ServiceName SERVICE_NAME = TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY;

    private UserTransactionRegistry userTransactionRegistry;

    public synchronized void start(StartContext context) throws StartException {
        userTransactionRegistry = new UserTransactionRegistry();
    }

    public synchronized void stop(StopContext context) {
        userTransactionRegistry = null;
    }

    public synchronized UserTransactionRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return userTransactionRegistry;
    }
}
