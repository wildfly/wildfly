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
package org.jboss.as.test.integration.ws.wsse.crypto;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.crypto.dsig.XMLSignatureFactory;

import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A testcase for verifying that XMLSignatureFactory can be properly created
 *
 * @author alessio.soldano@jboss.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XMLSignatureFactoryTestCase {

    private static Logger log = Logger.getLogger(XMLSignatureFactoryTestCase.class.getName());
    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "crypto.war").addClasses(TestServlet.class);
        return war;
    }

    @Test
    public void signedRequest() throws Exception {
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        Assert.assertNotNull(fac);

        BufferedReader br = new BufferedReader(new InputStreamReader(baseUrl.openStream()));
        try {
            Assert.assertEquals("OK", br.readLine());
        } finally {
            br.close();
        }
    }
}
