/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.xts.simple.wsat;

import com.arjuna.wst.*;
import org.jboss.logging.Logger;

import java.io.Serializable;

/**
 * An adapter class that exposes the RestaurantManager as a WS-T Atomic Transaction volatile participant.
 * 
 * @author paul.robinson@redhat.com, 2012-01-04
 * @author istudens@redhat.com
 */
public class RestaurantVolatileParticipant implements Volatile2PCParticipant, Serializable {
    private static final Logger log = Logger.getLogger(RestaurantVolatileParticipant.class);
    private static final long serialVersionUID = 1L;

    // The back-end resource for managing bookings
    private MockRestaurantManager mockRestaurantManager = MockRestaurantManager.getSingletonInstance();

    // The transaction ID for this transaction
    private String txID;

    /**
     * Creates a new participant for this transaction. Participants and transaction instances have a one-to-one mapping.
     *
     * @param txID the ID of the transaction tht this participant will be enlisted within.
     */
    public RestaurantVolatileParticipant(String txID) {
        this.txID = txID;
    }

    /**
     * Invokes the volatile prepare step of the business logic, reporting activity and outcome.
     * 
     * @return Prepared where possible, Aborted where necessary.
     * @throws com.arjuna.wst.WrongStateException
     * @throws com.arjuna.wst.SystemException
     */
    public Vote prepare() throws WrongStateException, SystemException {
        // Log the event and invoke the prepare operation
        // on the back-end logic.
        log.info("[SERVICE] Prepare called on volatile participant, about to prepare the back-end resource");
        boolean success = mockRestaurantManager.volatilePrepare(txID);

        // Map the return value from
        // the business logic to the appropriate Vote type.
        if (success) {
            log.info("[SERVICE] back-end resource prepared, volatile participant votes prepared");
            return new Prepared();
        } else {
            log.info("[SERVICE] back-end resource failed to prepare, volatile participant votes aborted");
            return new Aborted();
        }
    }

    /**
     * Invokes the volatile commit step of the business logic.
     * 
     * @throws com.arjuna.wst.WrongStateException
     * @throws com.arjuna.wst.SystemException
     */
    public void commit() throws WrongStateException, SystemException {
        // Log the event and invoke the commit operation
        // on the backend business logic.
        log.info("[SERVICE] all participants voted 'prepared', so coordinator tells the volatile participant that commit has been done");
        mockRestaurantManager.volatileCommit(txID);
    }

    /**
     * Invokes the volatile rollback operation on the business logic.
     * 
     * @throws com.arjuna.wst.WrongStateException
     * @throws com.arjuna.wst.SystemException
     */
    public void rollback() throws WrongStateException, SystemException {
        // Log the event and invoke the rollback operation
        // on the backend business logic.
        log.info("[SERVICE] one or more participants voted 'aborted' or a failure occurred, so coordinator tells the volatile participant that rollback has been done");
        mockRestaurantManager.volatileRollback(txID);
    }

    public void unknown() throws SystemException {
        log.info("RestaurantVolatileParticipantAT.unknown");
    }

    public void error() throws SystemException {
        log.info("RestaurantVolatileParticipantAT.error");
    }

}
