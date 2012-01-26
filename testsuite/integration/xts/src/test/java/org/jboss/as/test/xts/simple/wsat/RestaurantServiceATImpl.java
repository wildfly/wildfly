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
 * RestaurantServiceAT.java
 *
 * Copyright (c) 2003, 2004 Arjuna Technologies Ltd
 *
 * $Id: RestaurantServiceAT.java,v 1.3 2004/12/01 16:26:44 kconner Exp $
 *
 */
package org.jboss.as.test.xts.simple.wsat;

import org.jboss.as.test.xts.simple.wsat.jaxws.RestaurantServiceAT;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst11.TransactionManager;
import com.arjuna.mw.wst11.TransactionManagerFactory;
import com.arjuna.mw.wst11.UserTransactionFactory;

import org.jboss.logging.Logger;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.annotation.WebServlet;

/**
 * An adapter class that exposes the RestaurantManager business API as a transactional Web Service.
 * 
 * @author paul.robinson@redhat.com, 2012-01-04
 * 
 */
@WebService(serviceName = "RestaurantServiceATService", portName = "RestaurantServiceAT", name = "RestaurantServiceAT", targetNamespace = "http://www.jboss.com/jbossas/test/xts/simple/wsat/Restaurant")
@HandlerChain(file = "/context-handlers.xml", name = "Context Handlers")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebServlet(name="RestaurantServiceAT", urlPatterns={"/RestaurantServiceAT"})
public class RestaurantServiceATImpl implements RestaurantServiceAT {
    private static final Logger log = Logger.getLogger(RestaurantServiceATImpl.class);

    private MockRestaurantManager mockRestaurantManager = MockRestaurantManager.getSingletonInstance();

    /**
     * Book a number of seats in the restaurant. Enrols a Participant, then passes the call through to the business logic.
     */
    @WebMethod
    public void makeBooking() throws RestaurantException {

        log.info("[SERVICE] Restaurant service invoked to make a booking");
        String transactionId;
        try {
            // get the transaction ID associated with this thread
            transactionId = UserTransactionFactory.userTransaction().toString();

            // enlist the Participant for this service:
            TransactionManager transactionManager = TransactionManagerFactory.transactionManager();
            RestaurantParticipant restaurantParticipant = new RestaurantParticipant(transactionId);
            log.info("[SERVICE] Enlisting a Durable2PC participant into the AT");
            transactionManager.enlistForDurableTwoPhase(restaurantParticipant, "restaurantServiceAT:" + new Uid().toString());

            RestaurantVolatileParticipant restaurantVolatileParticipant = new RestaurantVolatileParticipant(transactionId);
            log.info("[SERVICE] Enlisting a Volatile2PC participant into the AT");
            transactionManager.enlistForVolatileTwoPhase(restaurantVolatileParticipant, "restaurantServiceAT:" + new Uid().toString());

        } catch (Exception e) {
            throw new RestaurantException("Error when enlisting participants", e);
        }

        // invoke the backend business logic:
        log.info("[SERVICE] Invoking the back-end business logic");
        mockRestaurantManager.makeBooking(transactionId);
    }

    /**
     * obtain the number of existing bookings
     * 
     * @return the number of current bookings
     */
    @WebMethod
    public int getBookingCount() {
        return mockRestaurantManager.getBookingCount();
    }

    /**
     * Reset the booking count to zero
     * 
     * Note: To simplify this example, this method is not part of the compensation logic, so will not be undone if the AT is
     * compensated. It can also be invoked outside of an active AT.
     */
    @WebMethod
    public void reset() {
        mockRestaurantManager.reset();
    }

    @WebMethod
    public boolean wasVolatileCommit() {
        return mockRestaurantManager.wasVolatileCommit();
    }

    @WebMethod
    public boolean wasVolatileRollback() {
        return mockRestaurantManager.wasVolatileRollback();
    }

}
