/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
