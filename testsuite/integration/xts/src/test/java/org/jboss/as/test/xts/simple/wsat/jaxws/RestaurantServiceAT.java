/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2010, Red Hat, and individual contributors
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
 * @author JBoss Inc.
 */
/*
 * RestaurantManager.java
 *
 * Copyright (c) 2003 Arjuna Technologies Ltd.
 *
 * $Id: RestaurantManager.java,v 1.3 2004/04/21 13:09:18 jhalliday Exp $
 *
 */
package org.jboss.as.test.xts.simple.wsat.jaxws;

import org.jboss.as.test.xts.simple.wsat.RestaurantException;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * Interface to a simple Restaurant. Provides simple methods to manipulate bookings.
 * 
 * @author paul.robinson@redhat.com, 2012-01-04
 */
@WebService(name = "RestaurantServiceAT", targetNamespace = "http://www.jboss.com/jbossas/test/xts/simple/wsat/Restaurant")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface RestaurantServiceAT {

    /**
     * Create a new booking
     */
    @WebMethod
    public void makeBooking() throws RestaurantException;

    /**
     * obtain the number of existing bookings
     * 
     * @return the number of current bookings
     */
    @WebMethod
    public int getBookingCount();

    /**
     * Reset the booking count to zero
     */
    @WebMethod
    public void reset();

    @WebMethod
    public boolean wasVolatileCommit();

    @WebMethod
    public boolean wasVolatileRollback();

}
