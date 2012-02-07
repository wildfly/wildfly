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

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst11.BusinessActivityManager;
import com.arjuna.mw.wst11.BusinessActivityManagerFactory;
import com.arjuna.wst.SystemException;
import org.jboss.as.test.xts.simple.wsba.AlreadyInSetException;
import org.jboss.as.test.xts.simple.wsba.MockSetManager;
import org.jboss.as.test.xts.simple.wsba.SetServiceException;
import org.jboss.as.test.xts.simple.wsba.coordinatorcompletion.jaxws.SetServiceBA;
import org.jboss.logging.Logger;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.annotation.WebServlet;

/**
 * An adapter class that exposes a set as a transactional Web Service.
 * 
 * @author Paul Robinson (paul.robinson@redhat.com)
 */
@WebService(serviceName = "SetServiceBAService", portName = "SetServiceBA", name = "SetServiceBA", targetNamespace = "http://www.jboss.com/jbossas/test/xts/simple/wsba/coordinatorcompletion")
@HandlerChain(file = "/context-handlers.xml", name = "Context Handlers")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebServlet(name="SetServiceBA", urlPatterns={"/SetServiceBA"})
public class SetServiceBAImpl implements SetServiceBA {
    private static final Logger log = Logger.getLogger(SetServiceBAImpl.class);

    /**
     * Add an item to a set and enroll a Participant if necessary then pass the call through to the business logic.
     * 
     * @param value the value to add to the set.
     * @throws AlreadyInSetException if value is already in the set
     * @throws SetServiceException if an error occurred when attempting to add the item to the set.
     */
    @WebMethod
    public void addValueToSet(String value) throws AlreadyInSetException, SetServiceException {

        log.info("[SERVICE] invoked addValueToSet('" + value + "')");

        BusinessActivityManager activityManager = BusinessActivityManagerFactory.businessActivityManager();

        /*
         * get the transaction context of this thread:
         */
        String transactionId;
        try {
            transactionId = activityManager.currentTransaction().toString();
        } catch (SystemException e) {
            throw new SetServiceException("Unable to lookup existing BusinesActivity", e);
        }

        /*
         * Lookup existing participant or register new participant
         */
        SetParticipantBA participantBA = SetParticipantBA.getParticipant(transactionId);

        if (participantBA == null) {
            try {
                // enlist the Participant for this service:
                SetParticipantBA participant = new SetParticipantBA(transactionId, value);
                SetParticipantBA.recordParticipant(transactionId, participant);

                log.info("[SERVICE] Enlisting a participant into the BA");
                activityManager.enlistForBusinessAgreementWithCoordinatorCompletion(participant, "SetServiceBAImpl:"
                        + new Uid().toString());
            } catch (Exception e) {
                log.error("Participant enlistment failed");
                throw new SetServiceException("Error enlisting participant", e);
            }
        } else {
            log.info("[SERVICE] Re-using the existing participant, already registered for this BA");
            participantBA.addValue(value);
        }

        // invoke the back-end business logic
        log.info("[SERVICE] Invoking the back-end business logic");
        MockSetManager.add(value);
    }

    /**
     * Query the set to see if it contains a particular value.
     * 
     * @param value the value to check for.
     * @return true if the value was present, false otherwise.
     */
    @WebMethod
    public boolean isInSet(String value) {
        return MockSetManager.isInSet(value);
    }

    /**
     * Empty the set
     * <p/>
     * Note: To simplify this example, this method is not part of the compensation logic, so will not be undone if the BA is
     * compensated. It can also be invoked outside of an active BA.
     */
    @WebMethod
    public void clear() {
        MockSetManager.clear();
    }
}
