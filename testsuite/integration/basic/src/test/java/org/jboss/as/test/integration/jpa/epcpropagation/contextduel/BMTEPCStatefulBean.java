/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation.contextduel;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;

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
