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
/*
 * RestaurantParticipantAT.java
 *
 * Copyright (c) 2003, 2004 Arjuna Technologies Ltd.
 *
 * $Id: RestaurantParticipantAT.java,v 1.3 2005/02/23 09:58:01 kconner Exp $
 *
 */
package org.jboss.as.test.xts.simple.wsat;

import com.arjuna.wst.*;
import org.jboss.logging.Logger;

import java.io.Serializable;

/**
 * An adapter class that exposes the RestaurantManager as a WS-T Atomic Transaction participant.
 * 
 * @author paul.robinson@redhat.com, 2012-01-04
 */
public class RestaurantParticipant implements Durable2PCParticipant, Serializable {
    private static final Logger log = Logger.getLogger(RestaurantParticipant.class);
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
    public RestaurantParticipant(String txID) {
        this.txID = txID;
    }

    /**
     * Invokes the prepare step of the business logic, reporting activity and outcome.
     * 
     * @return Prepared where possible, Aborted where necessary.
     * @throws com.arjuna.wst.WrongStateException
     * @throws com.arjuna.wst.SystemException
     */
    public Vote prepare() throws WrongStateException, SystemException {
        // Log the event and invoke the prepare operation
        // on the back-end logic.
        log.info("[SERVICE] Prepare called on participant, about to prepare the back-end resource");
        boolean success = mockRestaurantManager.prepare(txID);

        // Map the return value from
        // the business logic to the appropriate Vote type.
        if (success) {
            log.info("[SERVICE] back-end resource prepared, participant votes prepared");
            return new Prepared();
        } else {
            log.info("[SERVICE] back-end resource failed to prepare, participant votes aborted");
            return new Aborted();
        }
    }

    /**
     * Invokes the commit step of the business logic.
     * 
     * @throws com.arjuna.wst.WrongStateException
     * @throws com.arjuna.wst.SystemException
     */
    public void commit() throws WrongStateException, SystemException {
        // Log the event and invoke the commit operation
        // on the backend business logic.
        log.info("[SERVICE] all participants voted 'prepared', so coordinator tells the participant to commit");
        mockRestaurantManager.commit(txID);
    }

    /**
     * Invokes the rollback operation on the business logic.
     * 
     * @throws com.arjuna.wst.WrongStateException
     * @throws com.arjuna.wst.SystemException
     */
    public void rollback() throws WrongStateException, SystemException {
        // Log the event and invoke the rollback operation
        // on the backend business logic.
        log.info("[SERVICE] one or more participants voted 'aborted' or a failure occurred, so coordinator tells the participant to rollback");
        mockRestaurantManager.rollback(txID);
    }

    public void unknown() throws SystemException {
        log.info("RestaurantParticipantAT.unknown");
    }

    public void error() throws SystemException {
        log.info("RestaurantParticipantAT.error");
    }

}
