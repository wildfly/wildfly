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

package org.jboss.as.jpa.container;

import org.jboss.as.jpa.transaction.TransactionUtil;

import javax.ejb.EJBException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.TransactionRequiredException;
import java.util.Map;

/**
 * Transaction scoped entity manager will be injected into SLSB or SFSB beans.  At bean invocation time, they
 * will join the active transaction if one is present.  Otherwise, they will simply be cleared at the end of
 * the bean invocation.
 * <p/>
 * This is a proxy for the underlying persistent provider EntityManager.
 *
 * @author Scott Marlow
 */
public class TransactionScopedEntityManager extends AbstractEntityManager {

    private String puScopedName;          // Scoped name of the persistent unit
    private Map properties;
    private EntityManagerFactory emf;
    private boolean isExtendedPersistenceContext;
    private boolean isInTx;

    public TransactionScopedEntityManager(String puScopedName, Map properties, EntityManagerFactory emf) {
        this.puScopedName = puScopedName;
        this.properties = properties;
        this.emf = emf;
        setMetadata(puScopedName, false);
    }

    @Override
    protected EntityManager getEntityManager() {
        EntityManager result;

        // try to get EM from XPC and return it if puScopedName is found
        if ((result = SFSBCallStack.findPersistenceContext(puScopedName)) != null) {
            isExtendedPersistenceContext = true;    // using a XPC
            isInTx = TransactionUtil.getInstance().isInTx();
            if (isInTx) {
                // 7.6.3.1 throw EJBException if a different persistence context is already joined to the
                // transaction (with the same puScopedName).
                EntityManager existing = TransactionUtil.getInstance().getTransactionScopedEntityManager(puScopedName);
                if (existing != result) {       // should be enough to test if not the same object
                    throw new EJBException(
                        "Found extended persistence context in SFSB invocation call stack but that cannot be used " +
                        "because the transaction already has a transactional context associated with it.  " +
                        "This can be avoided by changing application code, either eliminate the extended " +
                        "persistence context or the transactional context.  See JPA spec 2.0 section 7.6.3.1.  " +
                        "Scoped persistence unit name=" +puScopedName);
                }
                TransactionUtil.getInstance().registerExtendedWithTransaction(puScopedName, result);
            }
        } else {
            isExtendedPersistenceContext = false;  // not using a XPC

            isInTx = TransactionUtil.getInstance().isInTx();
            if (isInTx) {
                result = TransactionUtil.getInstance().getOrCreateTransactionScopedEntityManager(emf, puScopedName, properties);
            } else {
                result = EntityManagerUtil.createEntityManager(emf, properties);
            }
        }
        setMetadata(puScopedName, false);    // save metadata if not already set
        return result;
    }

    @Override
    protected boolean isExtendedPersistenceContext() {
        return isExtendedPersistenceContext;
    }

    @Override
    protected boolean isInTx() {
        return isInTx;
    }

    /**
     * Catch the application trying to close the container managed entity manager and throw an IllegalStateException
     */
    @Override
    public void close() {
        // Transaction scoped entity manager will be closed when the (owning) component invocation completes
        throw new IllegalStateException("Container managed entity manager can only be closed by the container " +
            "(auto-cleared at tx/invocation end and closed when owning component is closed.)");

    }

}
