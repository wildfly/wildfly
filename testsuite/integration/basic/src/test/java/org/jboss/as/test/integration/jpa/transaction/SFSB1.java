/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction;

import java.util.HashSet;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.UserTransaction;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSB1 {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    @PersistenceContext(unitName = "deferdetachpc")
    EntityManager deferDetachEm;

    @Resource
    SessionContext sessionContext;

    // always throws a TransactionRequiredException
    public void createEmployeeNoTx(String name, String address, int id) {


        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.persist(emp);
            tx1.commit();
        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);
        }

        em.flush();         // should throw TransactionRequiredException
    }

    public Company createEmployee(String name, String address, int id) {


        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        Company theCompany = new Company();
        theCompany.setId(id);   // reuse the employee id
        HashSet set = new HashSet();
        set.add(emp);
        theCompany.setEmployees(set);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.persist(emp);
            em.persist(theCompany);
            tx1.commit();
        } catch (Exception e) {
            throw new RuntimeException("couldn't start tx", e);
        }
        return theCompany;
    }


    public Employee getEmployeeNoTX(int id) {

        return em.find(Employee.class, id);
    }

    public String queryEmployeeNameNoTX(int id) {
        Query q = em.createQuery("SELECT e.name FROM Employee e where e.id=:id");
        q.setParameter("id", new Integer(id));
        try {
            String name = (String) q.getSingleResult();
            return name;
        } catch (NoResultException expected) {
            return "success";
        } catch (Exception unexpected) {
            return unexpected.getMessage();
        }
    }

    public Employee queryEmployeeNoTX(int id) {
        TypedQuery<Employee> q = em.createQuery("SELECT e FROM Employee e where e.id=:id", Employee.class);
        q.setParameter("id", new Integer(id));
        return q.getSingleResult();
    }

    // return true if the queried Employee is detached as required by JPA 2.0 section 3.8.6
    // For a transaction scoped persistence context non Jakarta Transactions tx invocation, entities returned from Query
    // must be detached.
    public boolean isQueryEmployeeDetached(int id) {
        TypedQuery<Employee> q = em.createQuery("SELECT e FROM Employee e where e.id=:id", Employee.class);
        q.setParameter("id", new Integer(id));
        Employee employee = q.getSingleResult();
        return em.contains(employee) != true;
    }

    // return true if lazy associations are accessible as per the WFLY-3674 extension (jboss.as.jpa.deferdetach)
    public boolean isLazyAssociationAccessibleWithDeferredDetach(int id) {
        Company company = deferDetachEm.find(Company.class, id);
        return company.getEmployees().size() == 1;
    }

}
