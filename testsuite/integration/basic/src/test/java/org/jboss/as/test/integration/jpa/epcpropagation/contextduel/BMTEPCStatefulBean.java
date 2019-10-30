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

package org.jboss.as.test.integration.jpa.epcpropagation.contextduel;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

/**
 * Test JPA 2.0 section 7.6.3.1
 * "
 * If the component is a stateful session bean to which an extended persistence context has been
 * bound and there is a different persistence context bound to the JTA transaction,
 * an EJBException is thrown by the container.
 * "
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class BMTEPCStatefulBean {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "mypc")
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    @EJB(beanName = "CMTPCStatefulBean")
    CMTPCStatefulBean cmtpcStatefulBean;

    // expect EJBException to be thrown for success, anything else thrown is an error
    public void shouldThrowError() throws SystemException, NotSupportedException, RollbackException,
            HeuristicRollbackException, HeuristicMixedException {
        Employee emp = new Employee();
        emp.setAddress("1003 Main Street");
        emp.setName("Sharon Sells");
        emp.setId(1);
        em.persist(emp);  // XPC has new entity, not yet saved to db

        sessionContext.getUserTransaction().begin();
        // XPC is still isolated from this started TX
        // start normal persistence context action, which will first be associated with TX
        Employee ensureIsolation = cmtpcStatefulBean.getEmp(1);
        if (ensureIsolation != null) {
            // if XPC was saved to the DB, then the XPC support has a bug
            throw new RuntimeException("XPC leaked, should of been isolated from new transaction");
        }

        // the XPC is still isolated from the started TX, cmtpcStatefulBean.em should be associated with the TX
        // if we invoke an operation on the BMTEPCStatefulBean.em, an EJBException should be thrown
        Employee shouldOfBeenAnError = em.find(Employee.class, 1);
        throw new RuntimeException("EJBException not thrown when attempting to propagate EPC into TX that already has PC " +
                ", look at ExtendedEntityManager logic for likely cause, find returned = " + shouldOfBeenAnError
        );
        //sessionContext.getUserTransaction().commit();

    }

    public void shouldNotThrowError() throws SystemException, NotSupportedException, RollbackException,
            HeuristicRollbackException, HeuristicMixedException {
        Employee emp = new Employee();
        emp.setAddress("1003 Main Street");
        emp.setName("Sharon Sells");
        emp.setId(1);
        em.persist(emp);  // XPC has new entity, not yet saved to db

        sessionContext.getUserTransaction().begin();

        emp = em.find(Employee.class, 1);   // XPC should be associated with TX
        emp = em.find(Employee.class, 1);   // this second call would trigger AS7-1673

        if (emp == null) {
            throw new RuntimeException("expected XPC search to find entity");
        }

        // start normal persistence context action, which should use the XPC
        emp = cmtpcStatefulBean.getEmp(1);
        if (emp == null) {
            throw new RuntimeException("expected PC search to use XPC and find entity");
        }

        sessionContext.getUserTransaction().commit();

        emp = cmtpcStatefulBean.getEmp(1);   // XPC should be flushed to the db, search in new TX should work

        if (emp == null) {
            throw new RuntimeException("expected DB search with new TX, to find entity");
        }

    }

}
