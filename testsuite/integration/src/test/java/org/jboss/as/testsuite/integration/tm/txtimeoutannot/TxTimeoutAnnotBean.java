/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.tm.txtimeoutannot;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.tm.TxUtils;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.logging.Logger;

/**
 * Transaction Timeout Annotated Test Bean
 *
 * @author pskopek@redhat.com
 * @author istudens@redhat.com
 */
@Stateless
public class TxTimeoutAnnotBean {

    private static final Logger log = Logger.getLogger(TxTimeoutAnnotBean.class);


    /**
     * This method's timeout should be 5 secs
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(5)
    public void testOverriddenTimeoutExpires() {
        sleep(7000, false);
        int status = getTxStatus();
        log.info("testOverriddenTimeoutExpires: " + TxUtils.getStatusAsString(status));
        if (!TxUtils.isRollback(status)) {
            // give it a second chance
            sleep(2000, false);
            status = getTxStatus();
            log.info("testOverriddenTimeoutExpires: " + TxUtils.getStatusAsString(status));

            if (!TxUtils.isRollback(status))
                throw new EJBException("Should be marked rolled back: " + TxUtils.getStatusAsString(status));
        }
    }

    /**
     * This method's timeout should be 20 secs
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(20)
    public void testOverriddenTimeoutDoesNotExpire() {
        sleep(12000, true);
        int status = getTxStatus();
        if (status != Status.STATUS_ACTIVE)
            throw new EJBException("Should be active: " + TxUtils.getStatusAsString(status));
    }

    private int getTxStatus() {
        try {
            TransactionManager tm = (TransactionManager) new InitialContext().lookup("java:jboss/TransactionManager");
            return tm.getStatus();
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }


    private void sleep(int timeout, boolean throwEJBException) {
        try {
            Thread.sleep(timeout);
        } catch (Exception e) {
            if (throwEJBException)
                throw new EJBException(e);
            else
                log.debug("Ignored", e);
        }
    }
}
