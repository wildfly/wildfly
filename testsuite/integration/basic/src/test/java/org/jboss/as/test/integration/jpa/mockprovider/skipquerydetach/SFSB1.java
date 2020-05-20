/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2019, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.jpa.mockprovider.skipquerydetach;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

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
