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
