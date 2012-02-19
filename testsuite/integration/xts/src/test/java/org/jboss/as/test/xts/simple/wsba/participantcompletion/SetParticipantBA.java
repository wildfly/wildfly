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
package org.jboss.as.test.xts.simple.wsba.participantcompletion;

import com.arjuna.wst.BusinessAgreementWithParticipantCompletionParticipant;
import com.arjuna.wst.FaultedException;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.WrongStateException;
import com.arjuna.wst11.ConfirmCompletedParticipant;
import org.jboss.as.test.xts.simple.wsba.MockSetManager;
import org.jboss.logging.Logger;

import java.io.Serializable;

/**
 * An adapter class that exposes the SetManager as a WS-BA participant using the 'Participant Completion' protocol.
 * 
 * The Set Service only allows a single item to be added to the set in any given transaction. So, this means it can complete at
 * the end of the addValueToSet call, rather than having to wait for the coordinator to tell it to do so. Hence it uses a
 * participant which implements the 'participant completion' protocol.
 * 
 * @author Paul Robinson (paul.robinson@redhat.com)
 */
public class SetParticipantBA implements BusinessAgreementWithParticipantCompletionParticipant, ConfirmCompletedParticipant,
        Serializable {
    private static final Logger log = Logger.getLogger(SetParticipantBA.class);

    private static final long serialVersionUID = 1L;

    private String value;

    /**
     * Participant instances are related to business method calls in a one to one manner.
     * 
     * @param value the value to remove from the set during compensation
     */
    public SetParticipantBA(String value) {
        this.value = value;
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

        // Compensate work
        MockSetManager.rollback(value);
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

        // Compensate work done by the service
        MockSetManager.rollback(value);
    }

    public String status() {
        return null;
    }

    @Deprecated
    public void unknown() throws SystemException {
    }

    public void error() throws SystemException {
        log.info("[SERVICE] Participant.error");

        // Compensate work done by the service
        MockSetManager.rollback(value);
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
            MockSetManager.rollback(value);
        }
    }
}
