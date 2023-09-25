/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.epcpropagation.shallow;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

/**
 * DAO stateful bean that has its own isolated extended persistence context
 * its simbling bean will have its own instance of the extended persistence context
 *
 * @author Scott Marlow
 */
@Stateful
public class FirstDAO {

    @Resource
    SessionContext ctx;

    @PersistenceContext(unitName = "shallow_pu", type = PersistenceContextType.EXTENDED)
    private EntityManager em;

    public void noop() {

    }

    /**
     * no failures should occur during this test method.  If the extended persistence context is not shared as expected,
     * an EJBException will be thrown (indicating failure).
     */
    public void induceCreationViaJNDILookup() {

        SecondDAO secondDAO = (SecondDAO) ctx.lookup("java:module/SecondDAO");     // create an instance of SecondDAO that will share
        // the same extended persistence context


        secondDAO.noop();   // any failures should of already occurred, calling noop just gives an additional
        // change to check for other unexpected failures.

    }


    /**
     * no failures should occur during this test method.  If the extended persistence context is not shared as expected,
     * an EJBException will be thrown (indicating failure).
     */
    public void induceTwoLevelCreationViaJNDILookup() {

        SecondDAO secondDAO = (SecondDAO) ctx.lookup("java:module/SecondDAO");     // create an instance of SecondDAO that will share
        // the same extended persistence context


        secondDAO.induceCreationViaJNDILookup();    // second level of creation via JNDI lookup


    }


}
