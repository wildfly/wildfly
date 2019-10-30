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

package org.jboss.as.test.xts.wsba.participantcompletion.service;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.EventLog;
import org.jboss.as.test.xts.util.ServiceCommand;

import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst11.BusinessActivityManager;
import com.arjuna.mw.wst11.BusinessActivityManagerFactory;
import com.arjuna.wst.SystemException;
import com.arjuna.wst11.BAParticipantManager;

import javax.inject.Inject;

import static org.jboss.as.test.xts.util.ServiceCommand.*;

/**
 * Service implemenation - this implemetation is inherited by web services.
 */
public abstract class BAParticipantCompletionSuperService implements BAParticipantCompletion {
    private static final Logger log = Logger.getLogger(BAParticipantCompletionSuperService.class);

    // for tests this is not needed - but the code operating with participant registry should be synchronized
    private static final Map<String, BAParticipantManager> participantRegistry = new HashMap<String, BAParticipantManager>();

    @Inject
    private EventLog eventLog;


    /**
     * Add an item to a set Enrolls a Participant if necessary and passes the call through to the business logic.
     *
     * @param value the value to add to the set.
     * @throws
     * @throws AlreadyInSetException if value is already in the set
     * @throws SetServiceException   if an error occurred when attempting to add the item to the set.
     */
    public void saveData(String value, ServiceCommand... serviceCommands) throws TestApplicationException {

        log.trace("[BA PARTICIPANT COMPL SERVICE] invoked saveData('" + value + "')");
        eventLog.foundEventLogName(value);

        BAParticipantManager participantManager;
        BusinessActivityManager activityManager = BusinessActivityManagerFactory.businessActivityManager();
        String txid;

        try {
            txid = activityManager.currentTransaction().toString();
        } catch (SystemException se) {
            throw new RuntimeException("Error on getting TX id from BusinessActivityManager", se);
        }

        if (participantRegistry.keySet().contains(txid) && ServiceCommand.isPresent(REUSE_BA_PARTICIPANT, serviceCommands)) {
            log.trace("[BA PARTICIPANT COMPL SERVICE] Reusing BA participant manager - command: " + REUSE_BA_PARTICIPANT);
            participantManager = participantRegistry.get(txid);
        } else {
            try {
                // Enlist the Participant for this service:
                BAParticipantCompletionParticipant participant = new BAParticipantCompletionParticipant(serviceCommands, eventLog, value);
                log.trace("[BA PARTICIPANT COMPL SERVICE] Enlisting a participant into the BA");
                participantManager = activityManager.enlistForBusinessAgreementWithParticipantCompletion(participant,
                        "BAParticipantCompletition:" + new Uid().toString());
                participantRegistry.put(txid, participantManager);
            } catch (Exception e) {
                log.error("[BA PARTICIPANT COMPL SERVICE]  Participant enlistment failed", e);
                throw new RuntimeException("Error enlisting participant", e);
            }
        }

        if (ServiceCommand.isPresent(APPLICATION_EXCEPTION, serviceCommands)) {
            throw new TestApplicationException("Intentionally thrown Application Exception - service command set to: " + APPLICATION_EXCEPTION);
        }

        // There could be invoked some back-end business logic here
        /*
         * This service employs the participant completion protocol which means it decides when it wants to commit local
         * changes. If the local changes (adding the item to the set) succeeded, we notify the coordinator that we have
         * completed. Otherwise, we notify the coordinator that we cannot complete. If any other participant fails or the client
         * decides to cancel we can rely upon being told to compensate.
         */
        log.trace("[BA PARTICIPANT COMPL SERVICE] Prepare the backend resource and if successful notify the coordinator that we have completed our work");
        if (ServiceCommand.isPresent(DO_COMPLETE, serviceCommands)) {
            try {
                // Tell the coordinator manager we have finished our work
                log.trace("[BA PARTICIPANT COMPL SERVICE] Prepare successful, notifying coordinator of completion");
                participantManager.completed();
            } catch (Exception e) {
                /* Failed to notify the coordinator that we have finished our work. Compensate the work and throw an Exception
                 * to notify the client that the add operation failed. */
                log.error("[BA PARTICIPANT COMPL SERVICE] 'completed' callback failed");
                throw new RuntimeException("Error when notifying the coordinator that the work is completed", e);
            }
        }

        if (ServiceCommand.isPresent(CANNOT_COMPLETE, serviceCommands)) {
            try {
                // Tell the participant manager we cannot complete. This will force the activity to fail.
                log.trace("[BA PARTICIPANT COMPL SERVICE] Prepared fail, notifying coordinator that we cannot complete");
                participantManager.cannotComplete();
                return;
            } catch (Exception e) {
                log.error("[BA PARTICIPANT COMPL SERVICE] 'cannotComplete' callback failed");
                throw new RuntimeException("Error when notifying the coordinator that the work is cannot be completed", e);
            }
        }

    }
}
