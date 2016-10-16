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

package org.jboss.as.test.integration.ejb.transaction.usertransaction;

import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Jaikiran Pai
 * @author Eduardo Martins
 */
@Stateless
public class CMTSLSB {

    private static final Logger logger = Logger.getLogger(CMTSLSB.class);

    @Resource
    private EJBContext ejbContext;

    @EJB
    private BMTSLSB bmtSlsb;

    @PostConstruct
    void onConstruct() {
        this.checkUserTransactionAccess();
    }

    public void checkUserTransactionAccessDenial() {
        // check access to UserTransaction
        this.checkUserTransactionAccess();
        // now invoke a BMT SLSB and we expect that, that bean has access to the UserTransaction during the invocation
        this.bmtSlsb.checkUserTransactionAvailability();
        // now check UserTransaction access for ourselves (again) after the call to the BMT bean has completed
        this.checkUserTransactionAccess();
    }

    private void checkUserTransactionAccess() {
        // (JBoss specific) java:jboss/UserTransaction lookup string
        try {
            final String utJndiName = "java:jboss/UserTransaction";
            InitialContext.doLookup(utJndiName);
            throw new RuntimeException("UserTransaction lookup at " + utJndiName + " was expected to fail in a CMT bean");
        } catch (NamingException e) {
            // expected since it's in CMT
            logger.trace("Got the expected exception while looking up UserTransaction in CMT bean", e);
        }
        // (spec mandated) java:comp/UserTransaction lookup string
        try {
            final String utJavaCompJndiName = "java:comp/UserTransaction";
            InitialContext.doLookup(utJavaCompJndiName);
            throw new RuntimeException("UserTransaction lookup at " + utJavaCompJndiName + " was expected to fail in a CMT bean");
        } catch (NamingException e) {
            // expected since it's in CMT
            logger.trace("Got the expected exception while looking up UserTransaction in CMT bean", e);
        }
        // try invoking EJBContext.getUserTransaction()
        try {
            ejbContext.getUserTransaction();
            throw new RuntimeException("EJBContext.getUserTransaction() was expected to throw an exception when invoked from a CMT bean, but it didn't!");
        } catch (IllegalStateException ise) {
            // expected since it's in CMT
            logger.trace("Got the expected exception while looking up EJBContext.getUserTransaction() in CMT bean", ise);
        }
    }

}
