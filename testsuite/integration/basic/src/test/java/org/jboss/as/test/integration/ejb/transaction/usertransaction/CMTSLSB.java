/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.usertransaction;

import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;
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
