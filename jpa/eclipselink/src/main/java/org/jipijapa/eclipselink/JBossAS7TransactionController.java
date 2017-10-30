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

import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.eclipse.persistence.transaction.AbstractSynchronizationListener;
import org.eclipse.persistence.transaction.jboss.JBossTransactionController;
import org.jboss.logging.Logger;

/**
 * The transaction controller overrides some of the default behavior of eclipse
 * link under the classes
 * {@link org.eclipse.persistence.transaction.AbstractTransactionController} and
 * {@link org.eclipse.persistence.transaction.JTATransactionController} to
 * ensure that the unit of work synchronization is not taking place at too-late
 * point in time.
 *
 * <P>
 * REFERENCES: <br>
 *
 * <a href="https://issues.jboss.org/browse/WFLY-8954">Wildfly 10 with
 * eclipselink Onscucess observer gets stale entity</a>
 *
 */
public class JBossAS7TransactionController extends JBossTransactionController {

    private static final Logger LOGGER = Logger.getLogger(JBossAS7TransactionController.class);

    @Override
    protected void registerSynchronization_impl(AbstractSynchronizationListener listener, Object txn) throws Exception {
        try {
            // (a) Our first approach is to register the listener on the
            // transaction registry
            // to ensure that this listener during the oncomplete phase of a
            // transaction the weld CDI listener
            registerSynchronizationUsingTransactionRegistry((Synchronization) listener);
        } catch (Exception ingnoreE) {
            // (b) Make sure that this error is made visible - we want the
            // default approach to work 100% of the time - not just sometimes.
            // We know that the fallback approach will yield us the bug
            // See, WFLY-8954
            // eclipselink will register the listener on the Transaction instead
            // of on the TransactionRegistry
            String errMsg = String.format(
                    "Unexpected error took place while attempting to register the transaction listener: %1$s "
                            + " in the container transaction registry. %n " + " Error was: %2$s. %n "
                            + " As a fallback approach, we shall now attempt to register the transaction manager in the JTA transaction: %3$s. %n",
                    listener.getClass().getCanonicalName(), ingnoreE.getMessage(), txn);
            LOGGER.warn(errMsg, ingnoreE);
            super.registerSynchronization_impl(listener, txn);
        }
    }

    /**
     * Register the jta transaction synchronization listener in the container's
     * transaction registry.
     *
     * <P>
     * Motivation: <br>
     * There may be multiple transaction listeners that have an interest in
     * binding to the life cycle of a jta transaction. In some cases, the order
     * by which the listeners is triggered is not arbitrary. In particular, the
     * eclipselink synchronization listener must always be executed before the
     * CDI EJB event listener during the oncomplete phase of a JTA transaction.
     * Otherwise, the CDI observers may be handling stale objects.
     *
     * @param listener
     *            The listener will typically be the eclipselink
     *            {@link org.eclipse.persistence.transaction.JTASynchronizationListener}
     * @throws Exception
     *             Unexpected error take plade either during the process of
     *             lookup of the {@link TransactionSynchronizationRegistry} or
     *             in the process of registering the listener.
     */
    protected void registerSynchronizationUsingTransactionRegistry(Synchronization listener) throws Exception {
        TransactionSynchronizationRegistry transactionSynchronizationRegistry = JndiUtil.SINGLETON
                .acquireTransactionSynchronizationRegistry();
        transactionSynchronizationRegistry.registerInterposedSynchronization(listener);
    }

    // Jndi Resource acquisiation
    @Override
    protected TransactionManager acquireTransactionManager() throws Exception {
        try {
            // (a) The transaction manager is expected to be available in the
            // jndi tree of the container
            return JndiUtil.SINGLETON.acquireTransactionManager();
        } catch (NamingException ex) {
            // (b) Give indication of error and use a fall-back approach
            // NOTE: the fall-back approach - should never be needed
            LOGGER.error(ex.getMessage());
            return super.acquireTransactionManager();
        }
    }

}
