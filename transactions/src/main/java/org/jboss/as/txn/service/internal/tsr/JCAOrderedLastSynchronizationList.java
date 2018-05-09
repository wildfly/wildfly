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
package org.jboss.as.txn.service.internal.tsr;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.as.txn.logging.TransactionLogger;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.ContextTransactionSynchronizationRegistry;

/**
 * This class was added to:
 *
 * 1. workaround an issue discussed in https://java.net/jira/browse/JTA_SPEC-4 whereby the JCA Synchronization(s) need to be
 * called after the JPA Synchronization(s). Currently the implementation orders JCA relative to all interposed Synchronizations,
 * if this is not desirable it would be possible to modify this class to store just the JPA and JCA syncs and the other syncs
 * can simply be passed to a delegate (would need the reference to this in the constructor).
 *
 * 2. During afterCompletion the JCA synchronizations should be called last as that allows JCA to detect connection leaks from
 * frameworks that have not closed the JCA managed resources. This is described in (for example)
 * http://docs.oracle.com/javaee/5/api/javax/transaction/TransactionSynchronizationRegistry
 * .html#registerInterposedSynchronization(javax.transaction.Synchronization) where it says that during afterCompletion
 * "Resources can be closed but no transactional work can be performed with them"
 */
public class JCAOrderedLastSynchronizationList implements Synchronization {
    private final List<Synchronization> preJcaSyncs = new ArrayList<Synchronization>();
    private final List<Synchronization> jcaSyncs = new ArrayList<Synchronization>();

    public JCAOrderedLastSynchronizationList() {
    }

    /**
     * This is only allowed at various points of the transaction lifecycle.
     *
     * @param synchronization The synchronization to register
     * @throws IllegalStateException In case the transaction was in a state that was not valid to register under
     * @throws SystemException In case the transaction status was not known
     */
    public void registerInterposedSynchronization(Synchronization synchronization) throws IllegalStateException, SystemException {
        int status = ContextTransactionSynchronizationRegistry.getInstance().getTransactionStatus();
        switch (status) {
            case javax.transaction.Status.STATUS_ACTIVE:
            case javax.transaction.Status.STATUS_PREPARING:
                break;
            case Status.STATUS_MARKED_ROLLBACK:
                // do nothing; we can pretend like it was registered, but it'll never be run anyway.
                return;
            default:
                throw TransactionLogger.ROOT_LOGGER.syncsnotallowed(status);
        }
        if (synchronization.getClass().getName().startsWith("org.jboss.jca")) {
            if (TransactionLogger.ROOT_LOGGER.isTraceEnabled()) {
                TransactionLogger.ROOT_LOGGER.trace("JCAOrderedLastSynchronizationList.jcaSyncs.add - Class: " + synchronization.getClass() + " HashCode: "
                    + synchronization.hashCode() + " toString: " + synchronization);
            }
            jcaSyncs.add(synchronization);

        } else {
            if (TransactionLogger.ROOT_LOGGER.isTraceEnabled()) {
                TransactionLogger.ROOT_LOGGER.trace("JCAOrderedLastSynchronizationList.preJcaSyncs.add - Class: " + synchronization.getClass() + " HashCode: "
                    + synchronization.hashCode() + " toString: " + synchronization);
            }
            preJcaSyncs.add(synchronization);
        }
    }

    /**
     * Exceptions from Synchronizations that are registered with this TSR are not trapped for before completion. This is because
     * an error in a Sync here should result in the transaction rolling back.
     *
     * You can see that in effect in these classes:
     * https://github.com/jbosstm/narayana/blob/5.0.4.Final/ArjunaCore/arjuna/classes
     * /com/arjuna/ats/arjuna/coordinator/TwoPhaseCoordinator.java#L91
     * https://github.com/jbosstm/narayana/blob/5.0.4.Final/ArjunaJTA
     * /jta/classes/com/arjuna/ats/internal/jta/resources/arjunacore/SynchronizationImple.java#L76
     */
    @Override
    public void beforeCompletion() {
        // This is needed to guard against syncs being registered during the run, otherwise we could have used an iterator
        int lastIndexProcessed = 0;
        while ((lastIndexProcessed < preJcaSyncs.size())) {
            Synchronization preJcaSync = preJcaSyncs.get(lastIndexProcessed);
            if (TransactionLogger.ROOT_LOGGER.isTraceEnabled()) {
                TransactionLogger.ROOT_LOGGER.trace("JCAOrderedLastSynchronizationList.preJcaSyncs.before_completion - Class: " + preJcaSync.getClass() + " HashCode: "
                    + preJcaSync.hashCode()
                    + " toString: "
                    + preJcaSync);
            }
            preJcaSync.beforeCompletion();
            lastIndexProcessed = lastIndexProcessed + 1;
        }

        // Do the same for the jca syncs
        lastIndexProcessed = 0;
        while ((lastIndexProcessed < jcaSyncs.size())) {
            Synchronization jcaSync = jcaSyncs.get(lastIndexProcessed);
            if (TransactionLogger.ROOT_LOGGER.isTraceEnabled()) {
                TransactionLogger.ROOT_LOGGER.trace("JCAOrderedLastSynchronizationList.jcaSyncs.before_completion - Class: " + jcaSync.getClass() + " HashCode: "
                    + jcaSync.hashCode()
                    + " toString: "
                    + jcaSync);
            }
            jcaSync.beforeCompletion();
            lastIndexProcessed = lastIndexProcessed + 1;
        }
    }

    @Override
    public void afterCompletion(int status) {
        // The list should be iterated in reverse order - has issues with EJB3 if not
        // https://github.com/jbosstm/narayana/blob/master/ArjunaCore/arjuna/classes/com/arjuna/ats/arjuna/coordinator/TwoPhaseCoordinator.java#L509
        for (int i = preJcaSyncs.size() - 1; i>= 0; --i) {
            Synchronization preJcaSync = preJcaSyncs.get(i);
            if (TransactionLogger.ROOT_LOGGER.isTraceEnabled()) {
                TransactionLogger.ROOT_LOGGER.trace("JCAOrderedLastSynchronizationList.preJcaSyncs.afterCompletion - Class: " + preJcaSync.getClass() + " HashCode: "
                    + preJcaSync.hashCode()
                    + " toString: " + preJcaSync);
            }
            try {
                preJcaSync.afterCompletion(status);
            } catch (Exception e) {
                // Trap these exceptions so the rest of the synchronizations get the chance to complete
                // https://github.com/jbosstm/narayana/blob/5.0.4.Final/ArjunaJTA/jta/classes/com/arjuna/ats/internal/jta/resources/arjunacore/SynchronizationImple.java#L102
                TransactionLogger.ROOT_LOGGER.preJcaSyncAfterCompletionFailed(preJcaSync, ContextTransactionManager.getInstance().getTransaction(), e);
            }
        }
        for (int i = jcaSyncs.size() - 1; i>= 0; --i) {
            Synchronization jcaSync = jcaSyncs.get(i);
            if (TransactionLogger.ROOT_LOGGER.isTraceEnabled()) {
                TransactionLogger.ROOT_LOGGER.trace("JCAOrderedLastSynchronizationList.jcaSyncs.afterCompletion - Class: " + jcaSync.getClass() + " HashCode: "
                    + jcaSync.hashCode()
                    + " toString: "
                    + jcaSync);
            }
            try {
                jcaSync.afterCompletion(status);
            } catch (Exception e) {
                // Trap these exceptions so the rest of the synchronizations get the chance to complete
                // https://github.com/jbosstm/narayana/blob/5.0.4.Final/ArjunaJTA/jta/classes/com/arjuna/ats/internal/jta/resources/arjunacore/SynchronizationImple.java#L102
                TransactionLogger.ROOT_LOGGER.jcaSyncAfterCompletionFailed(jcaSync, ContextTransactionManager.getInstance().getTransaction(), e);
            }
        }
    }
}
