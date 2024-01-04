/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.txnaccess;

import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@Stateless
public class CMTSLSB {

    private static final Logger logger = Logger.getLogger(CMTSLSB.class);

    @PostConstruct
    void onConstruct() {
        this.checkUserTransactionAccess();
    }

    public void checkUserTransactionAccessDenial() {
        this.checkUserTransactionAccess();
    }

    private void checkUserTransactionAccess() {
        try {
            final String remoteUserTransactionName = "txn:RemoteUserTransaction";
            InitialContext.doLookup(remoteUserTransactionName);
            throw new RuntimeException("UserTransaction lookup at " + remoteUserTransactionName + " was expected to fail in a CMT bean");
        } catch (NamingException e) {
            // expected since it's in CMT
            logger.trace("Got the expected exception while looking up UserTransaction in CMT bean", e);
        }
        try {
            final String localUserTransactionName = "txn:LocalUserTransaction";
            InitialContext.doLookup(localUserTransactionName);
            throw new RuntimeException("UserTransaction lookup at " + localUserTransactionName + " was expected to fail in a CMT bean");
        } catch (NamingException e) {
            // expected since it's in CMT
            logger.trace("Got the expected exception while looking up UserTransaction in CMT bean", e);
        }
    }

}
