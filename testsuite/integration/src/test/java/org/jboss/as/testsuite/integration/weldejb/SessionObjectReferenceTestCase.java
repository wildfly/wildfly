/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.testsuite.integration.weldejb;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.beans.XMLDecoder;
import java.io.StringBufferInputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.ejb.ConcurrentAccessException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.stdio.WriterOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test two things:
 * 1. EJBTHREE-697: First concurrent call doesn't throw exception
 * 2. make sure the SFSB is instantiated in the same thread as the Servlet, so propagation works
 *
 * Make sure a concurrent call to a SFSB proxy over Weld gets a ConcurrentAccessException.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class SessionObjectReferenceTestCase {
    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war")
            .addClasses(HttpRequest.class, SimpleServlet.class, SimpleStatefulSessionBean.class, WriterOutputStream.class)
            .addWebResource(SessionObjectReferenceTestCase.class.getPackage().getName() + "/beans.xml")
            .addWebResource(SessionObjectReferenceTestCase.class.getPackage().getName() + "/web.xml")
            ;
        return war;
    }

    private static String performCall(String urlPattern, String param) {
        String spec = "http://localhost:8080/war-example/" + urlPattern + "?input=" + param;
        try {
            return HttpRequest.get(spec, 30, SECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEcho() throws Exception {
        String result = performCall("simple", "Hello+world");
        XMLDecoder decoder = new XMLDecoder(new StringBufferInputStream(result));
        List<String> results = (List<String>) decoder.readObject();
        List<Exception> exceptions = (List<Exception>) decoder.readObject();
        String sharedContext = (String) decoder.readObject();
        decoder.close();

        assertEquals(1, results.size());
        assertEquals("Echo Hello world", results.get(0));
        assertEquals(1, exceptions.size());
        assertTrue(exceptions.get(0) instanceof ConcurrentAccessException);
        assertEquals("Shared context", sharedContext);
    }
}
