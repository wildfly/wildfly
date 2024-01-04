/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.txbridge.fromjta.service;

import jakarta.ejb.Remote;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * Interface to a simple First. Provides simple methods to manipulate with counter.
 */
@WebService(name = "FirstServiceAT", targetNamespace = "http://www.jboss.com/jbossas/test/txbridge/fromjta/first")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@Remote
public interface FirstServiceAT {

    /**
     * Create a new booking
     */
    @WebMethod
    public void incrementCounter(int numSeats);

    /**
     * Obtain the number of existing bookings
     */
    @WebMethod
    public int getCounter();

    /**
     * Reset the booking count to zero
     */
    @WebMethod
    public void resetCounter();

}
