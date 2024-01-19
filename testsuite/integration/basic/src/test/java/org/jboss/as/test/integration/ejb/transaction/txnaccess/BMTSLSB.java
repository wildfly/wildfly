/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.txnaccess;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.UserTransaction;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@Stateless
@TransactionManagement(value = TransactionManagementType.BEAN)
public class BMTSLSB {

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
        final String remoteUserTransactionName = "txn:RemoteUserTransaction";
        final UserTransaction remoteUserTransaction = InitialContext.doLookup(remoteUserTransactionName);
        if (remoteUserTransaction == null) {
            throw new RuntimeException("UserTransaction lookup at " + remoteUserTransactionName + " returned null in a BMT bean");
        }
        final String localUserTransactionName = "txn:LocalUserTransaction";
        final UserTransaction localUserTransaction = InitialContext.doLookup(localUserTransactionName);
        if (localUserTransaction == null) {
            throw new RuntimeException("UserTransaction lookup at " + localUserTransaction + " returned null in a BMT bean");
        }
    }
}
