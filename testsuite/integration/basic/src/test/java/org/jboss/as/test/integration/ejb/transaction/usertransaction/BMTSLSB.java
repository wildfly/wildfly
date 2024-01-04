/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.usertransaction;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.UserTransaction;

/**
 * @author Jaikiran Pai
 * @author Eduardo Martins
 */
@Stateless
@TransactionManagement(value = TransactionManagementType.BEAN)
public class BMTSLSB {

    @Resource
    private EJBContext ejbContext;

    @Resource
    private UserTransaction userTransaction;

    @PostConstruct
    void onConstruct() {
        try {
            this.checkUserTransactionAccess();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkUserTransactionAvailability() {
        try {
            this.checkUserTransactionAccess();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

    }

    private void checkUserTransactionAccess() throws NamingException {
        if (userTransaction == null) {
            throw new RuntimeException("UserTransaction injection in a BMT bean was expected to happen successfully, but it didn't!");
        }
        // (JBoss specific) java:jboss/UserTransaction lookup string
        final String utJndiName = "java:jboss/UserTransaction";
        final UserTransaction userTxInJBossNamespace = InitialContext.doLookup(utJndiName);
        if (userTxInJBossNamespace == null) {
            throw new RuntimeException("UserTransaction lookup at " + utJndiName + " returned null in a BMT bean");
        }
        // (spec mandated) java:comp/UserTransaction lookup string
        final String utJavaCompJndiName = "java:comp/UserTransaction";
        final UserTransaction userTxInJavaCompNamespace = InitialContext.doLookup(utJavaCompJndiName);
        if (userTxInJavaCompNamespace == null) {
            throw new RuntimeException("UserTransaction lookup at " + utJavaCompJndiName + " returned null in a BMT bean");
        }
        // try invoking EJBContext.getUserTransaction()
        final UserTransaction ut = ejbContext.getUserTransaction();
        if (ut == null) {
            throw new RuntimeException("EJBContext.getUserTransaction() returned null in a BMT bean");
        }
    }
}
