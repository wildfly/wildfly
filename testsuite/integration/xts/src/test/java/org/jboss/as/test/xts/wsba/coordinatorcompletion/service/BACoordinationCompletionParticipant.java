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

package org.jboss.as.test.xts.wsba.coordinatorcompletion.service;

import com.arjuna.wst.BusinessAgreementWithCoordinatorCompletionParticipant;
import com.arjuna.wst.FaultedException;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.WrongStateException;
import com.arjuna.wst11.ConfirmCompletedParticipant;
import org.jboss.as.test.xts.util.EventLog;
import org.jboss.as.test.xts.util.EventLogEvent;
import org.jboss.as.test.xts.util.ServiceCommand;
import org.jboss.logging.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * An adapter class that exposes the SetManager as a WS-BA participant using the 'Coordinator Completion' protocol.
 * <p/>
 * The Set Service can be invoked multiple times to add many items to the set within a single BA. The service waits for the
 * coordinator to tell it to complete. This has the advantage that the client can continue calling methods on the service right
 * up until it calls 'close'. However, any resources held by the service need to be held for this duration, unless the service
 * decides to autonomously cancel the BA.
 * 
 * @author Paul Robinson <paul.robinson@redhat.com>
 * @author Ondrej Chaloupka <ochaloup@redhat.com> 
 */
public class BACoordinationCompletionParticipant implements BusinessAgreementWithCoordinatorCompletionParticipant, ConfirmCompletedParticipant,
        Serializable {
    private static final Logger log = Logger.getLogger(BACoordinationCompletionParticipant.class);
    private static final long serialVersionUID = 1L;
    // The ID of the corresponding transaction
    private String txID;
    // A list of values added to the set. These are removed from the set at
    // compensation time.
    private List<String> values = new LinkedList<String>();
    // table of currently active participants
    private static HashMap<String, Set<BACoordinationCompletionParticipant>> participants = new HashMap<String, Set<BACoordinationCompletionParticipant>>();
    
    private String participantName; 
    // Service command which define behaving of the participant
    private ServiceCommand[] serviceCommands;
    // Where to log participant activity
    private EventLog eventLog;

    /**
     * Participant instances are related to business method calls in a one to one manner.
     * 
     * @param txID The ID of the current Business Activity
     * @param value the value to remove from the set during compensation
     */
    public BACoordinationCompletionParticipant(ServiceCommand[] serviceCommands, EventLog eventLog, String txID, String value) {
        this.txID = txID;
        this.serviceCommands = serviceCommands;
        this.eventLog = eventLog;
        this.participantName = value;
        addValue(value);
    }

    /**
     * Notify the participant that another value is being added to the set. This is stored in case compensation is required.
     * 
     * @param value the value being added to the set
     */
    public void addValue(String value) {
        values.add(value);
    }

    @Override
    public String status() {
        return null;
    }
    
    /**
     * The transaction has completed successfully. The participant previously informed the coordinator that it was ready to
     * complete.
     * 
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException never in this implementation.
     */
    public void close() throws WrongStateException, SystemException {
        // Nothing to do here as the item has already been added to the set
        eventLog.addEvent(participantName, EventLogEvent.CLOSE);
        log.info("[BA COORDINATOR COMPL SERVICE] Participant close() - logged: " + EventLogEvent.CLOSE);
        // The participant knows that this BA is now finished and can throw away any temporary state
        removeParticipant(txID, this);
    }

    /**
     * The transaction has canceled, and the participant should undo any work. The participant cannot have informed the
     * coordinator that it has completed.
     *
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException never in this implementation.
     */
    public void cancel() throws WrongStateException, SystemException {
        eventLog.addEvent(participantName, EventLogEvent.CANCEL);
        log.info("[BA COORDINATOR COMPL SERVICE] Participant cancel() - logged: " + EventLogEvent.CANCEL);
        // the participant should compensate any work done within this BA here
        removeParticipant(txID, this);
    }

    /**
     * The transaction has cancelled. The participant previously informed the coordinator that it had finished work but could
     * compensate later if required, and it is now requested to do so.
     *
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException if unable to perform the compensating transaction.
     */
    public void compensate() throws FaultedException, WrongStateException, SystemException {
        eventLog.addEvent(participantName, EventLogEvent.COMPENSATE);
        log.info("[BA COORDINATOR COMPL SERVICE] Participant compensate() - logged: " + EventLogEvent.COMPENSATE);
        // there will be carrying out some compensation action here
        removeParticipant(txID, this);
    }

    @Deprecated
    public void unknown() throws SystemException {
        eventLog.addEvent(participantName, EventLogEvent.UNKNOWN);
        log.info("[BA COORDINATOR COMPL SERVICE] Participant unknown() - logged: " + EventLogEvent.UNKNOWN);
        removeParticipant(txID, this);
    }

    public void error() throws SystemException {
        eventLog.addEvent(participantName, EventLogEvent.ERROR);
        log.info("[BA COORDINATOR COMPL SERVICE] Participant error() - logged: " + EventLogEvent.ERROR);
        removeParticipant(txID, this);
    }

    public void complete() throws WrongStateException, SystemException {
        // This tells the participant that the BA completed, but may be compensated later
        eventLog.addEvent(participantName, EventLogEvent.COMPLETE);
        log.info("[BA COORDINATOR COMPL SERVICE] Participant complete() - logged: " + EventLogEvent.COMPLETE);
        
        if(ServiceCommand.isPresent(ServiceCommand.SYSTEM_EXCEPTION_ON_COMPLETE, serviceCommands)) {
            log.info("[BA COORDINATOR COMPL SERVICE] Participant complete() - intentionally throwing " + SystemException.class.getName());
            throw new SystemException("Intentionally throwing system exception to get compensation method on run");
        }
    }

    /**
     * Method called to perform commit or rollback of prepared changes to the underlying manager state after the participant
     * recovery record has been written
     * 
     * @param confirmed true if the log record has been written and changes should be rolled forward and false if it has not
     *        been written and changes should be rolled back
     */
    public void confirmCompleted(boolean confirmed) {
        if (confirmed) {
            // This tells the participant that compensation information has been logged and that it is safe to commit any changes
            eventLog.addEvent(participantName, EventLogEvent.CONFIRM_COMPLETED);
            log.info("[BA COORDINATOR COMPL SERVICE] Participant confirmCompleted(true) - logged: " + EventLogEvent.CONFIRM_COMPLETED);
        } else {
            // A compensation action will follow here
        	eventLog.addEvent(participantName, EventLogEvent.CONFIRM_FAILED);
        	log.info("[BA COORDINATOR COMPL SERVICE] Participant confirmCompleted(false) - logged: " + EventLogEvent.CONFIRM_FAILED);
        }
    }

    /************************************************************************/
    /* tracking active participants */
    /************************************************************************/
    /**
     * keep track of a participant
     * 
     * @param txID the participant's transaction id
     * @param participant The participant associated with this BA
     */
    public static synchronized void recordParticipant(String txID, BACoordinationCompletionParticipant participant) {
        getParticipantSet(txID).add(participant);
    }

    /**
     * forget about a participant
     * 
     * @param txID the participant's transaction id
     */
    public static void removeParticipant(String txID, BACoordinationCompletionParticipant participant) {
        getParticipantSet(txID).remove(participant);
        if(getParticipantSet(txID).isEmpty()) {
            participants.remove(txID);
        }
    }

    /**
     * lookup a participant
     * 
     * @param txID the participant's transaction id
     * @return the participant
     */
    public static synchronized BACoordinationCompletionParticipant getSomeParticipant(String txID) {
        Iterator<BACoordinationCompletionParticipant> i = getParticipantSet(txID).iterator();
        if(i.hasNext()) {
            return i.next();
        }
        return null;
    }
    
    private static Set<BACoordinationCompletionParticipant> getParticipantSet(String txID) {
        if(participants.containsKey(txID)) {
            return participants.get(txID);
        } else {
            Set<BACoordinationCompletionParticipant> set = new HashSet<BACoordinationCompletionParticipant>();
            participants.put(txID, set);
            return set;
        }
    }
}
