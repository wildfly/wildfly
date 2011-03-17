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
        EntityManager result = null;

        // try to get EM from XPC and return it if puScopedName is found
        // TODO:  look in XPC for specified puScopedName
        // set isExtendedPersistenceContext if XPC is used

        isExtendedPersistenceContext = false;  // not using a XPC

        isInTx = TransactionUtil.getInstance().isInTx();
        if (isInTx) {
            result = TransactionUtil.getInstance().getTransactionScopedEntityManager(emf, puScopedName, properties);
        }
        else {
            result = EntityManagerUtil.createEntityManager(emf, properties);
        }
        setMetadata(puScopedName,false);    // save metadata if not already set
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
