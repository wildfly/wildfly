/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.epcpropagation.shallow;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

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
