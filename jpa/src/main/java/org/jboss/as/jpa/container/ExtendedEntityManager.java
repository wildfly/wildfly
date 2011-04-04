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
import java.io.Serializable;

/**
 * Extended lifetime scoped (XPC) entity manager will only be injected into SFSB beans.  At bean invocation time, they
 * will join the active JTA transaction if one is present.  If not active JTA transaction is present,
 * created/deleted/updated/loaded entities will remain associated with the entity manager until it is joined with a
 * transaction (commit will save the changes, rollback will lose them).
 * <p/>
 * At injection time, a persistence context will be associated with the SFSB.
 * During a SFSB1 invocation, if a new SFSB2 is created with an XPC referencing the same
 * persistence unit, the new SFSB2 will share the same persistence context from SFSB1.
 * Both SFSB1 + SFSB2 will maintain a reference to the underlying persistence context, such that
 * the underlying persistence context will be kept around until both SFSB1 + SFSB2 are destroyed.
 *
 * Note:  Unlike TransactionScopedEntityManager, ExtendedEntityManager will directly be shared instead of the
 * underlying EntityManager.  This will facilitate access to the EntityManagerMetadata used in SFSBXPCMap.
 *
 * @author Scott Marlow
 */
public class ExtendedEntityManager extends AbstractEntityManager implements Serializable {

    private static final long serialVersionUID = 432435L;

    /**
     * EntityManager obtained from the persistence provider that represents the XPC.
     */

    private EntityManager underlyingEntityManager;

    private String puScopedName;

    public ExtendedEntityManager(final String puScopedName, final EntityManager underlyingEntityManager) {
        super(puScopedName, true);
        this.underlyingEntityManager = underlyingEntityManager;
        this.puScopedName = puScopedName;
    }

    /**
     * See org.jboss.ejb3.stateful.EJB3XPCResolver.getExtendedPersistenceContext() for AS6 implementation.
     * The JPA SFSB interceptor will track the stack of SFSB invocations.  The underlying EM will be obtained from
     * the current SFSB being invoked (via our JPA SFSB interceptor).
     *
     * @return
     */
    @Override
    protected EntityManager getEntityManager() {
        return underlyingEntityManager;
    }

    @Override
    protected boolean isExtendedPersistenceContext() {
        return true;
    }

    @Override
    protected boolean isInTx() {
        return TransactionUtil.getInstance().isInTx();
    }

    /**
     * Catch the application trying to close the container managed entity manager and throw an IllegalStateException
     */
    @Override
    public void close() {
        // An extended entity manager will be closed when the EJB SFSB @remove method is invoked.
        throw new IllegalStateException("Container managed entity manager can only be closed by the container " +
            "(will happen when @remove method is invoked on containing SFSB)");

    }

    public void containerClose() {
        underlyingEntityManager.close();
    }

    @Override
    public String toString() {
        return "ExtendedEntityManager [" + puScopedName +"]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExtendedEntityManager that = (ExtendedEntityManager) o;

        if (!puScopedName.equals(that.puScopedName)) return false;
        if (!underlyingEntityManager.equals(that.underlyingEntityManager)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = underlyingEntityManager.hashCode();
        result = 31 * result + puScopedName.hashCode();
        return result;
    }

}
