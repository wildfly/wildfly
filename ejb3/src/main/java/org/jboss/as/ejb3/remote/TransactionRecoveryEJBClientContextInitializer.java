/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientContextInitializer;

/**
 * Registers a {@link org.jboss.ejb.client.EJBClientContextListener} which will keep track of the EJB receivers that have been added to the EJB client contexts. Those receivers will then be used to query the server for
 * any recoverable Xid(s)
 *
 * @author Jaikiran Pai
 */
public class TransactionRecoveryEJBClientContextInitializer implements EJBClientContextInitializer {
    @Override
    public void initialize(EJBClientContext context) {
        context.registerEJBClientContextListener(EJBTransactionRecoveryService.INSTANCE);
        EjbLogger.ROOT_LOGGER.debugf("Registered %s as a listener to EJB client context %s", EJBTransactionRecoveryService.INSTANCE, context);
    }
}
