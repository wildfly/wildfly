/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jipijapa.eclipselink;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * Helper class that centralizes the jndi lookup logic.
 */
public final class JndiUtil {

    public static final JndiUtil SINGLETON = new JndiUtil();

    /**
     * Create a new JndiUtil.
     */
    private JndiUtil() {

    }

    /**
     * Obtain the container transaction manager.
     *
     * @return The container transaction manager (e.g.
     *         com.arjuna.ats.jbossatx.jta.TransactionManagerDelegate)
     * @throws Exception
     *             Unexpected error takes place during jndi lookup
     */
    public TransactionManager acquireTransactionManager() throws Exception {
        try {
            return InitialContext.doLookup(JndiConstants.JBOSS_TRANSACTION_MANAGER);
        } catch (NamingException ex) {
            String errMsg = createErrMsgForFailedJndiLookup(ex, JndiConstants.JBOSS_TRANSACTION_MANAGER);
            throw new Exception(errMsg, ex);
        }
    }

    /**
     * Obtain the transaction synchronization registry from the container via
     * jndi lookup.
     *
     * @return The transaction synchronization registry (e.g.
     *         org.jboss.as.txn.service.internal.tsr.TransactionSynchronizationRegistryWrapper)
     *
     * @throws Exception
     *             A checked exception is fired whenever an unexpected error
     *             takes place (e.g. the jndi lookup of
     *             {@link JndiConstants#JTA_TRANSACTION_SYNCHRONIZATION_REGISTRY}
     *             fails)
     */
    public TransactionSynchronizationRegistry acquireTransactionSynchronizationRegistry() throws Exception {
        try {
            return InitialContext.doLookup(JndiConstants.JTA_TRANSACTION_SYNCHRONIZATION_REGISTRY);
        } catch (NamingException ex) {
            // (a) Build an error message
            // note: in this case we do not log the erro - since pump up an
            // exception
            // there is no need to do log spamming
            String errMsg = createErrMsgForFailedJndiLookup(ex, JndiConstants.JTA_TRANSACTION_SYNCHRONIZATION_REGISTRY);

            // (b) Attempt break the execution logic accessing this resource is
            // critical.
            throw new Exception(errMsg, ex);
        }
    }

    /**
     * Build an error message giving an indication that a resource expected to
     * exist in the container and be accessible via jndi lookup was not found.
     *
     * @param namingException
     *            A jndi lookup error
     * @param jndiLookupName
     *            The jndi name that could not be looked up
     * @return An indicative error message
     */
    private String createErrMsgForFailedJndiLookup(NamingException namingException, String jndiLookupName) {
        return String.format(
                "Unexpected error took place while attempting to lookup the container TransactionSynchronizationRegistry. %n"
                        + " Jndi-lookup-name: %1$s %n" + " Error was: %2$s. ",
                jndiLookupName, namingException.getMessage());

    }

}
