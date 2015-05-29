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

package org.jboss.as.jpa.container;

import java.util.Map;

import javax.transaction.SystemException;

/**
 * Listens for UserTransaction events and handles associating the extended persistence context with the JTA transaction.
 *
 * JPA 2.0 section 7.9.1 Container Responsibilities:
 *  "For stateful session beans with extended persistence contexts:
 *    When a business method of the stateful session bean is invoked, if the stateful session bean
 *    uses bean managed transaction demarcation and a UserTransaction is begun within the
 *    method, the container associates the persistence context with the JTA transaction and calls
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
