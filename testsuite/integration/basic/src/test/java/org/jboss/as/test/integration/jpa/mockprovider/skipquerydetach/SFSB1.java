/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.mockprovider.skipquerydetach;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.Query;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSB1 {
    @PersistenceUnit(unitName = "queryresultsaremanaged")
    EntityManagerFactory emf;

    @PersistenceContext(unitName = "queryresultsaremanaged")
    EntityManager em;

    @PersistenceContext(unitName = "queryresultsaredetached")
    EntityManager emQueryResultsAreDetached;

    /**
     * verify that entitymanager.clear is not called after executing returned query.
     *
     * Return null for success, otherwise for test failure return fail message.
     */
    public String queryWithSkipQueryDetachEnabled() {
        if(TestEntityManager.getInvocations().contains("close")) {
            return "invalid state, entity manager has already been previously closed";
        }
        if(TestEntityManager.getInvocations().contains("clear")) {
            return "invalid state, entity manager has already been previously cleared";
        }
        Query query =  em.createQuery("mock query");
        if(TestEntityManager.getInvocations().contains("close")) {
            return "invalid state, entity manager was closed before query was executed, " +
                    "which means that 'wildfly.jpa.skipquerydetach=true' couldn't work, as the " +
                    "persistence context was closed too early";
        }
        if(TestEntityManager.getInvocations().contains("clear")) {
            return "invalid state, entity manager was cleared (detached) before query was executed, " +
                    "which means that 'wildfly.jpa.skipquerydetach=true' didn't work";
        }
        query.getSingleResult();
        if(TestEntityManager.getInvocations().contains("close")) {
            return "invalid state, entity manager was closed before SFSB1 bean call completed, " +
                    "which means that 'wildfly.jpa.skipquerydetach=true' couldn't work, as the " +
                    "persistence context was closed too early";
        }
        if(TestEntityManager.getInvocations().contains("clear")) {
            return "invalid state, entity manager was cleared (detached) before SFSB1 bean call completed, " +
                    "which means that 'wildfly.jpa.skipquerydetach=true' didn't work";
        }
        return null;  // success
    }

    /**
     * verify that entitymanager.clear is called after executing returned query.
     *
     * Return null for success, otherwise for test failure return fail message.
     */
    public String queryWithSkipQueryDetachDisabled() {
        if(TestEntityManager.getInvocations().contains("close")) {
            return "invalid state, entity manager has already been previously closed";
        }
        if(TestEntityManager.getInvocations().contains("clear")) {
            return "invalid state, entity manager has already been previously cleared";
        }
        Query query =  emQueryResultsAreDetached.createQuery("mock query");
        if(TestEntityManager.getInvocations().contains("clear")) {
            return "invalid state, entity manager was cleared (detached) after query was created but before executed," +
                    "which means that 'wildfly.jpa.skipquerydetach=false' (default) behaviour didn't work";
        }
        query.getSingleResult();
        if(TestEntityManager.getInvocations().contains("close")) {
            return "invalid state, entity manager was closed before SFSB1 bean call completed, " +
                    "which means that 'wildfly.jpa.skipquerydetach=false' couldn't work, as the " +
                    "persistence context was closed too early";
        }
        if(!TestEntityManager.getInvocations().contains("clear")) {
            return "invalid state, entity manager was not cleared (detached) as expected," +
                    "which means that 'wildfly.jpa.skipquerydetach=false' (default) behaviour didn't work";
        }
        return null;  // success
    }

}
