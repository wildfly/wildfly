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

import com.arjuna.wst.BusinessAgreementWithParticipantCompletionParticipant;
import com.arjuna.wst.FaultedException;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.WrongStateException;
import com.arjuna.wst11.ConfirmCompletedParticipant;

import org.jboss.as.test.xts.base.BaseFunctionalTest;
import org.jboss.as.test.xts.util.EventLog;
import org.jboss.as.test.xts.util.EventLogEvent;
import org.jboss.as.test.xts.util.ServiceCommand;
import org.jboss.logging.Logger;

import java.io.Serializable;

/**
 * A participant completion participant which only logs invoked methods. The log {@link EventLog} is then checked at the end of every test.
 *
 * @see BaseFunctionalTest#assertEventLog
 */
public class BAParticipantCompletionParticipant
        implements BusinessAgreementWithParticipantCompletionParticipant, ConfirmCompletedParticipant, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(BAParticipantCompletionParticipant.class);

    private String participantName;
    // Where to log participant activity
    private EventLog eventLog;


    /**
     * Participant instances are related to business method calls in a one to one manner.
     *
     * @param value the value to remove from the set during compensation
     */
    public BAParticipantCompletionParticipant(ServiceCommand[] serviceCommands, EventLog eventLog, String value) {
        this.participantName = value;
        this.eventLog = eventLog;
    }

    public String status() {
        return null;
    }

    /**
     * The transaction has completed successfully. The participant previously informed the coordinator that it was ready to
     * complete.
     *
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException     never in this implementation.
     */

    public void close() throws WrongStateException, SystemException {
        eventLog.addEvent(participantName, EventLogEvent.CLOSE);
        // The participant knows that this BA is now finished and can throw away any temporary state
        // nothing to do here as the item has already been added to the set
        log.trace("[BA PARTICIPANT COMPL SERVICE] Participant close() - logged: " + EventLogEvent.CLOSE);
    }

    /**
     * The transaction has canceled, and the participant should undo any work. The participant cannot have informed the
     * coordinator that it has completed.
     *
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException     never in this implementation.
     */

    public void cancel() throws WrongStateException, SystemException {
        eventLog.addEvent(participantName, EventLogEvent.CANCEL);
        // The participant should compensate any work done within this BA
        log.trace("[BA PARTICIPANT COMPL SERVICE] Participant cancel() - logged: " + EventLogEvent.CANCEL);
        // A compensate work will be carrying here
    }

    /**
     * The transaction has cancelled. The participant previously informed the coordinator that it had finished work but could
     * compensate later if required, and it is now requested to do so.
     *
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException     if unable to perform the compensating transaction.
     */

    public void compensate() throws FaultedException, WrongStateException, SystemException {
        eventLog.addEvent(participantName, EventLogEvent.COMPENSATE);
        log.trace("[BA PARTICIPANT COMPL SERVICE] Participant compensate() - logged: " + EventLogEvent.COMPENSATE);
        // A compensate work will be carrying here
    }

    @Deprecated
    public void unknown() throws SystemException {
        eventLog.addEvent(participantName, EventLogEvent.UNKNOWN);
        log.trace("[BA PARTICIPANT COMPL SERVICE] Participant unknown() - logged: " + EventLogEvent.UNKNOWN);
    }

    public void error() throws SystemException {
        eventLog.addEvent(participantName, EventLogEvent.ERROR);
        log.trace("[BA PARTICIPANT COMPL SERVICE] Participant error() - logged: " + EventLogEvent.ERROR);
        // A compensate work will be carrying here
    }

    /**
     * method called to perform commit or rollback of prepared changes to the underlying manager state after the participant
     * recovery record has been written
     *
     * @param confirmed true if the log record has been written and changes should be rolled forward and false if it has not
     *                  been written and changes should be rolled back
     */
    public void confirmCompleted(boolean confirmed) {
        log.trace("[BA PARTICIPANT COMPL SERVICE] Participant confirmCompleted(" + Boolean.toString(confirmed) + ")");
        if (confirmed) {
            // This tells the participant that compensation information has been logged and that it is safe to commit any changes
            eventLog.addEvent(participantName, EventLogEvent.CONFIRM_COMPLETED);
            log.trace("[BA PARTICIPANT COMPL SERVICE] Participant confirmCompleted(true) - logged: " + EventLogEvent.CONFIRM_COMPLETED);
        } else {
            eventLog.addEvent(participantName, EventLogEvent.CONFIRM_FAILED);
            log.trace("[BA PARTICIPANT COMPL SERVICE] Participant confirmCompleted(false) - logged: " + EventLogEvent.CONFIRM_FAILED);
        }
    }
}
