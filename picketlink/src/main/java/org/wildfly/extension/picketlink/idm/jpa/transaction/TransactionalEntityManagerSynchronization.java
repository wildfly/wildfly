/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.picketlink.idm.jpa.transaction;

import org.jboss.tm.TxUtils;

import javax.persistence.EntityManager;
import javax.transaction.Synchronization;

/**
 * <p>{@link javax.transaction.Synchronization} that knows how to close a transactional {@link javax.persistence.EntityManager}
 * once the transaction finishes.</p>
 *
 * @author Pedro Igor
 */
public class TransactionalEntityManagerSynchronization implements Synchronization {

    private final EntityManager entityManager;

    TransactionalEntityManagerSynchronization(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void beforeCompletion() {

    }

    @Override
    public void afterCompletion(int status) {
        if (safeToClose()) {
            this.entityManager.close();
        }
    }

    /**
     * AS7-6586 requires that the container avoid closing the EntityManager while the application
     * may be using the EntityManager in a different thread.  If the transaction has been rolled
     * back, will check if the current thread is the Arjuna transaction manager Reaper thread.  It is not
     * safe to call EntityManager.close from the Reaper thread, so false is returned.
     *
     * TODO: switch to depend on JBTM-1556 instead of checking the current thread name.
     *
     * @return
     */
    private boolean safeToClose() {
        if (this.entityManager.isOpen()) {
            return !TxUtils.isTransactionManagerTimeoutThread();
        }

        return true;
    }

}
