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

/**
 * Holder of Jndi constants.
 */
public interface JndiConstants {

    /**
     * Jndi name to lookup the container transaction manager (e.g.
     * com.arjuna.ats.jbossatx.jta.TransactionManagerDelegate).
     *
     * <P>
     * The outcome of this lookup is expected to be an object of
     * {@link javax.transaction.TransactionManager}
     *
     * @see javax.transaction.TransactionManager
     */
    String JBOSS_TRANSACTION_MANAGER = "java:jboss/TransactionManager";

    /**
     * Jndi name to lookup the container transaction synchronization registry
     * (e.g.
     * org.jboss.as.txn.service.internal.tsr.TransactionSynchronizationRegistryWrapper).
     *
     * <P>
     * Motivation: <br>
     * Eclipselink wishes to bind to specific life cycles of a JTA transaction,
     * e.g. it cares about the completion of a transaction to synchronize
     * changes on a unit of work with the server session cache. There are two
     * ways by which eclipselink can do this. One is by registering using the
     * {@link javax.transaction.Transaction#registerSynchronization(javax.transaction.Synchronization)}.
     * This is the traditional approach followed by eclipselink. It has the
     * disadvantage that it cannot guarantee that its logic will take place
     * before, for example, the CDI Synchroinizaton components. <br>
     * Another approach, is to register itself via
     * {@link javax.transaction.TransactionSynchronizationRegistry#registerInterposedSynchronization(javax.transaction.Synchronization)}.
     * This latter API would allow to prioritize the logic of the eclipselink
     * synchronization on the container.
     *
     *
     *
     *
     * <P>
     * The outcome of this lookup is expected to be an object of
     * {@link javax.transaction.TransactionSynchronizationRegistry}
     *
     * @see javax.transaction.TransactionSynchronizationRegistry
     */
    String JTA_TRANSACTION_SYNCHRONIZATION_REGISTRY = "java:jboss/TransactionSynchronizationRegistry";
}
