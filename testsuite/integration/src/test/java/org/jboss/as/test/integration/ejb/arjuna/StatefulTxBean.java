/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.arjuna;

// import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
// import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.TransactionManager;
// import org.jboss.ejb3.annotation.JndiInject;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateful(name = "StatefulTx")
@Remote(StatefulTx.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class StatefulTxBean implements StatefulTx {
    private static final Logger log = Logger.getLogger(StatefulTxBean.class);

    // @JndiInject(jndiName="java:/TransactionManager") private TransactionManager tm;
    public TransactionManager getTransactionManager() throws javax.transaction.SystemException {
        try {
            return (TransactionManager) new InitialContext().lookup("java:jboss/TransactionManager");
        } catch(NamingException e) {
            throw new javax.transaction.SystemException("Caused by NamingException during search of TransactionManager");
        }

    }

    @PersistenceContext(unitName = "test")
    private EntityManager manager;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean clear(Entity entity) throws javax.transaction.SystemException {
        entity = manager.find(Entity.class, entity.getId());
        if (entity != null)
            manager.remove(entity);

        return getReturn();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean persist(Entity entity) throws javax.transaction.SystemException {
        manager.persist(entity);

        return getReturn();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean isArjunaTransactedRequired() throws javax.transaction.SystemException {
        return getReturn();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean isArjunaTransactedRequiresNew() throws javax.transaction.SystemException {
        return getReturn();
    }

    protected boolean getReturn() throws javax.transaction.SystemException {
        if (getTransactionManager().getTransaction() == null)
            return false;

        if (!getTransactionManager().getClass().toString().contains("arjuna"))
            return false;

        if (!getTransactionManager().getTransaction().getClass().toString().contains("arjuna"))
            return false;

        log.error("tm.class: " + getTransactionManager().getClass().toString());
        log.error("tm.transaction.class: " + getTransactionManager().getTransaction().getClass().toString());
        return true;
    }

}
