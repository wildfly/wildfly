/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.util.Map;

import jakarta.transaction.SystemException;

/**
 * Listens for UserTransaction events and handles associating the extended persistence context with the Jakarta Transactions transaction.
 *
 * JPA 2.0 section 7.9.1 Container Responsibilities:
 *  "For stateful session beans with extended persistence contexts:
 *    When a business method of the stateful session bean is invoked, if the stateful session bean
 *    uses bean managed transaction demarcation and a UserTransaction is begun within the
 *    method, the container associates the persistence context with the Jakarta Transactions transaction and calls
 *    EntityManager.joinTransaction.
 *  "
 *
 * @author Scott Marlow
 */
public class JPAUserTransactionListener implements org.jboss.tm.usertx.UserTransactionListener {

    @Override
    public void userTransactionStarted() throws SystemException {

        Map<String, ExtendedEntityManager> currentActiveEntityManagers = SFSBCallStack.currentSFSBCallStackInvocation();
        if (currentActiveEntityManagers != null && currentActiveEntityManagers.size() > 0) {
            for (ExtendedEntityManager extendedEntityManager: currentActiveEntityManagers.values()) {
                extendedEntityManager.internalAssociateWithJtaTx();
            }
        }
    }
}
