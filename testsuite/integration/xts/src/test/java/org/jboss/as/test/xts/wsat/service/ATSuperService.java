/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.xts.wsat.service;

import javax.inject.Inject;

import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.EventLog;
import org.jboss.as.test.xts.util.ServiceCommand;

import static org.jboss.as.test.xts.util.ServiceCommand.*;

import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst11.TransactionManager;
import com.arjuna.mw.wst11.TransactionManagerFactory;
import com.arjuna.mw.wst11.UserTransaction;
import com.arjuna.mw.wst11.UserTransactionFactory;
import com.arjuna.wst.UnknownTransactionException;
import com.arjuna.wst.WrongStateException;

/**
 * Service implemenation - this implemetation is inherited by annotated web services.
 */
public abstract class ATSuperService implements AT {
    private static final Logger log = Logger.getLogger(ATSuperService.class);

    @Inject
    private EventLog eventLog;

    /**
     * Adding 2 participants - Volatile and Durable
     *
     * @param callName        call name works for differentiate several calls to the same webservice
     *                        if you don't want care pass null (or overloaded method :)
     * @param serviceCommands service commands that service will react on
     * @throws WrongStateException
     * @throws com.arjuna.wst.SystemException
     * @throws UnknownTransactionException
     * @throws SecurityException
     * @throws javax.transaction.SystemException
     * @throws IllegalStateException
     */
    public void invokeWithCallName(String callName, ServiceCommand[] serviceCommands) throws TestApplicationException {

        log.infof("[AT SERVICE] web method invoke(%s) with eventLog %s", callName, eventLog);
        eventLog.foundEventLogName(callName);
        UserTransaction userTransaction;

        try {
            userTransaction = UserTransactionFactory.userTransaction();
            String transactionId = userTransaction.transactionIdentifier();
            log.debug("RestaurantServiceAT transaction id =" + transactionId);

            // Enlist the Durable Participant for this service
            TransactionManager transactionManager = TransactionManagerFactory.transactionManager();
            ATDurableParticipant durableParticipant = new ATDurableParticipant(serviceCommands, callName, eventLog, transactionId);
            log.trace("[SERVICE] Enlisting a Durable2PC participant into the AT");
            transactionManager.enlistForDurableTwoPhase(durableParticipant, "ATServiceDurable:" + new Uid().toString());

            // Enlist the Volatile Participant for this service
            ATVolatileParticipant volatileParticipant = new ATVolatileParticipant(serviceCommands, callName, eventLog, transactionId);
            log.trace("[SERVICE] Enlisting a VolatilePC participant into the AT");
            transactionManager.enlistForVolatileTwoPhase(volatileParticipant, "ATServiceVolatile:" + new Uid().toString());
        } catch (Exception e) {
            throw new RuntimeException("Error when enlisting participants", e);
        }

        if (ServiceCommand.isPresent(APPLICATION_EXCEPTION, serviceCommands)) {
            throw new TestApplicationException("Intentionally thrown Application Exception - service command was set to: " + APPLICATION_EXCEPTION);
        }

        if (ServiceCommand.isPresent(ROLLBACK_ONLY, serviceCommands)) {
            log.trace("Intentionally the service settings transaction to rollback only - service command was set to: " + ROLLBACK_ONLY);
            try {
                userTransaction.rollback();
            } catch (Exception e) {
                throw new RuntimeException("The rollback is not possible", e);
            }
        }

        // There will be some business logic here normally
        log.trace("|AT SERVICE] I'm working on nothing...");
    }
}
