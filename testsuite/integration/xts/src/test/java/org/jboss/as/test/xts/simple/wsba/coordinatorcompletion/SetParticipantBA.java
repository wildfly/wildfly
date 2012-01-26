/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. 
 * See the copyright.txt in the distribution for a full listing 
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 * 
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
package org.jboss.as.test.xts.simple.wsba.coordinatorcompletion;

import com.arjuna.wst.BusinessAgreementWithCoordinatorCompletionParticipant;
import com.arjuna.wst.FaultedException;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.WrongStateException;
import com.arjuna.wst11.ConfirmCompletedParticipant;
import org.jboss.as.test.xts.simple.wsba.MockSetManager;
import org.jboss.logging.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * An adapter class that exposes the SetManager as a WS-BA participant using the 'Coordinator Completion' protocol.
 * <p/>
 * The Set Service can be invoked multiple times to add many items to the set within a single BA. The service waits for the
 * coordinator to tell it to complete. This has the advantage that the client can continue calling methods on the service right
 * up until it calls 'close'. However, any resources held by the service need to be held for this duration, unless the service
 * decides to autonomously cancel the BA.
 * 
 * @author Paul Robinson (paul.robinson@redhat.com)
 */
public class SetParticipantBA implements BusinessAgreementWithCoordinatorCompletionParticipant, ConfirmCompletedParticipant,
        Serializable {
    private static final Logger log = Logger.getLogger(SetParticipantBA.class);
    private static final long serialVersionUID = 1L;
    // The ID of the corresponding transaction
    private String txID;
    // A list of values added to the set. These are removed from the set at
    // compensation time.
    private List<String> values = new LinkedList<String>();
    // table of currently active participants
    private static HashMap<String, SetParticipantBA> participants = new HashMap<String, SetParticipantBA>();

    /**
     * Participant instances are related to business method calls in a one to one manner.
     * 
     * @param txID The ID of the current Business Activity
     * @param value the value to remove from the set during compensation
     */
    public SetParticipantBA(String txID, String value) {
        this.txID = txID;
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

    /**
     * The transaction has completed successfully. The participant previously informed the coordinator that it was ready to
     * complete.
     * 
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException never in this implementation.
     */
    public void close() throws WrongStateException, SystemException {
        // nothing to do here as the item has already been added to the set
        log.info("[SERVICE] Participant.close (The participant knows that this BA is now finished and can throw away any temporary state)");
        removeParticipant(txID);
    }

    /**
     * The transaction has canceled, and the participant should undo any work. The participant cannot have informed the
     * coordinator that it has completed.
     *
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException never in this implementation.
     */
    public void cancel() throws WrongStateException, SystemException {
        log.info("[SERVICE] Participant.cancel (The participant should compensate any work done within this BA)");
        doCompensate();
        removeParticipant(txID);
    }

    /**
     * The transaction has cancelled. The participant previously informed the coordinator that it had finished work but could
     * compensate later if required, and it is now requested to do so.
     *
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException if unable to perform the compensating transaction.
     */
    public void compensate() throws FaultedException, WrongStateException, SystemException {
        log.info("[SERVICE] Participant.compensate");
        doCompensate();
        removeParticipant(txID);
    }

    public String status() {
        return null;
    }

    public void unknown() throws SystemException {
        removeParticipant(txID);
    }

    public void error() throws SystemException {
        log.info("[SERVICE] Participant.error");
        doCompensate();
        removeParticipant(txID);
    }

    private void doCompensate() {
        log.info("[SERVICE] SetParticipantBA: Carrying out compensation action");
        for (String value : values) {
            MockSetManager.rollback(value);
        }
    }

    public void complete() throws WrongStateException, SystemException {
        log.info("[SERVICE] Participant.complete (This tells the participant that the BA completed, but may be compensated later)");
    }

    /**
     * method called to perform commit or rollback of prepared changes to the underlying manager state after the participant
     * recovery record has been written
     * 
     * @param confirmed true if the log record has been written and changes should be rolled forward and false if it has not
     *        been written and changes should be rolled back
     */
    public void confirmCompleted(boolean confirmed) {
        if (confirmed) {
            log.info("[SERVICE] Participant.confirmCompleted('"
                    + confirmed
                    + "') (This tells the participant that compensation information has been logged and that it is safe to commit any changes.)");
            MockSetManager.commit();
        } else {
            doCompensate();
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
    public static synchronized void recordParticipant(String txID, SetParticipantBA participant) {
        participants.put(txID, participant);
    }

    /**
     * forget about a participant
     * 
     * @param txID the participant's transaction id
     */
    public static void removeParticipant(String txID) {
        participants.remove(txID);
    }

    /**
     * lookup a participant
     * 
     * @param txID the participant's transaction id
     * @return the participant
     */
    public static synchronized SetParticipantBA getParticipant(String txID) {
        return participants.get(txID);
    }
}
