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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

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
