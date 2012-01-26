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
package org.jboss.as.test.xts.simple.wsba.coordinatorcompletion.jaxws;

import org.jboss.as.test.xts.simple.wsba.AlreadyInSetException;
import org.jboss.as.test.xts.simple.wsba.SetServiceException;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * Interface implemented by SetServiceBA Web service and Client stub.
 * 
 * The Web service represents a simple set collection and this interface provides the basic methods for accessing it.
 * 
 * @author paul.robinson@redhat.com, 2011-12-21
 */
@WebService(name = "SetServiceBA", targetNamespace = "http://www.jboss.com/jbossas/test/xts/simple/wsba/coordinatorcompletion")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface SetServiceBA {

    /**
     * Add a value to the set
     * 
     * @param value Value to add to the set.
     * @throws AlreadyInSetException if the item is already in the set.
     * @throws SetServiceException if an error occurred during the adding of the item to the set.
     */
    @WebMethod
    public void addValueToSet(String value) throws AlreadyInSetException, SetServiceException;

    /**
     * Query the set to see if it contains a particular value.
     * 
     * @param value the value to check for.
     * @return true if the value was present, false otherwise.
     */
    @WebMethod
    public boolean isInSet(String value);

    /**
     * Empty the set
     */
    @WebMethod
    public void clear();

}
