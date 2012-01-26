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
package org.jboss.as.test.xts.simple.wsat.client;

import com.arjuna.mw.wst11.client.JaxWSHeaderContextProcessor;

import org.jboss.as.test.xts.simple.wsat.RestaurantException;
import org.jboss.as.test.xts.simple.wsat.jaxws.RestaurantServiceAT;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A Client stub to the restaurant service.
 * 
 * @author paul.robinson@redhat.com, 2012-01-04
 */
@ClientStub
public class Client implements RestaurantServiceAT {
    private RestaurantServiceAT restaurant;

    /**
     * Default constructor with hard-coded values for the RestaurantServiceAT endpoint details (wsdl url, service name & port
     * name)
     * 
     * @throws java.net.MalformedURLException if the WSDL url is malformed.
     */
    public Client() throws MalformedURLException {
        String node0 = System.getProperty("node0", "127.0.0.1");

        URL wsdlLocation = new URL("http://" + node0 + ":8080/" + WSATTestCase.DEPLOYMENT_NAME + "/RestaurantServiceAT?wsdl");
        QName serviceName = new QName("http://www.jboss.com/jbossas/test/xts/simple/wsat/Restaurant",
                "RestaurantServiceATService");
        QName portName = new QName("http://www.jboss.com/jbossas/test/xts/simple/wsat/Restaurant", "RestaurantServiceAT");

        Service service = Service.create(wsdlLocation, serviceName);
        restaurant = service.getPort(portName, RestaurantServiceAT.class);

        /*
         * Add client handler chain
         */
        BindingProvider bindingProvider = (BindingProvider) restaurant;
        List<Handler> handlers = new ArrayList<Handler>(1);
        handlers.add(new JaxWSHeaderContextProcessor());
        bindingProvider.getBinding().setHandlerChain(handlers);
    }

    /**
     * Create a new booking
     */
    public void makeBooking() throws RestaurantException {
        restaurant.makeBooking();
    }

    /**
     * obtain the number of existing bookings
     * 
     * @return the number of current bookings
     */
    public int getBookingCount() {
        return restaurant.getBookingCount();
    }

    /**
     * Reset the booking count to zero
     */
    public void reset() {
        restaurant.reset();
    }

    public boolean wasVolatileCommit() {
        return restaurant.wasVolatileCommit();
    }

    public boolean wasVolatileRollback() {
        return restaurant.wasVolatileRollback();
    }

}
