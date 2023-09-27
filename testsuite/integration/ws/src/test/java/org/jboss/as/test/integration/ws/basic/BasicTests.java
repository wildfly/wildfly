/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.basic;

import java.util.Iterator;
import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.soap.SOAPFaultException;

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
