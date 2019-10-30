/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.basic;

import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
public abstract class BasicTests {

    protected EndpointIface proxy;

    @Test
    public void testHelloString() {
        Assert.assertEquals("Hello World!", proxy.helloString("World"));
    }

    @Test
    public void testHelloBean() {
        HelloObject helloObject = new HelloObject("Kermit");
        Assert.assertEquals("Hello Kermit!", proxy.helloBean(helloObject).getMessage());
    }

    @Test
    public void testHelloArray() {
        HelloObject[] query = new HelloObject[3];
        query[0] = new HelloObject("Kermit");
        query[1] = new HelloObject("Piggy");
        query[2] = new HelloObject("Fozzy");
        HelloObject[] reply = proxy.helloArray(query);
        for (int i = 0; i < reply.length; i++) {
            Assert.assertEquals("Hello " + query[i].getMessage() + "!", reply[i].getMessage());
        }
    }

    @Test
    public void testHelloError() {
        try {
            proxy.helloError("Fault for test purpose");
            Assert.fail("This should throw a SOAPFaultException");
        } catch (SOAPFaultException ex) {
            SOAPFault fault = ex.getFault();
            Assert.assertEquals("Fault for test purpose", fault.getFaultString());
            Iterator iter = fault.getFaultSubcodes();
            Assert.assertTrue(iter != null);
            Assert.assertTrue(iter.hasNext());
            QName subcode = (QName) iter.next();
            Assert.assertEquals(new QName("http://ws.gss.redhat.com/", "NullPointerException"), subcode);
            Assert.assertTrue(iter.hasNext());
            subcode = (QName) iter.next();
            Assert.assertEquals(new QName("http://ws.gss.redhat.com/", "OperatorNotFound"), subcode);
        }
    }

}
