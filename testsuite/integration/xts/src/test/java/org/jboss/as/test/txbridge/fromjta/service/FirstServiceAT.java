/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
