/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.junit.Test;

/**
 * @author alessio.soldano@jboss.com
 * @since 18-Jan-2011
 */
public class WSDeploymentAspectParserTestCase {

    @Test
    public void test() throws Exception {
        InputStream is = getXmlUrl("jbossws-deployment-aspects-example.xml").openStream();
        try {
            List<DeploymentAspect> das = WSDeploymentAspectParser.parse(is, this.getClass().getClassLoader());
            assertEquals(4, das.size());
            boolean da1Found = false;
            boolean da2Found = false;
            boolean da3Found = false;
            boolean da4Found = false;
            for (DeploymentAspect da : das) {
                if (da instanceof TestDA2) {
                    da2Found = true;
                    TestDA2 da2 = (TestDA2) da;
                    assertEquals("myString", da2.getTwo());
                } else if (da instanceof TestDA3) {
                    da3Found = true;
                    TestDA3 da3 = (TestDA3) da;
                    assertNotNull(da3.getList());
                    assertTrue(da3.getList().contains("One"));
                    assertTrue(da3.getList().contains("Two"));
                } else if (da instanceof TestDA4) {
                    da4Found = true;
                    TestDA4 da4 = (TestDA4) da;
                    assertEquals(true, da4.isBool());
                    assertNotNull(da4.getMap());
                    assertEquals(1, (int) da4.getMap().get("One"));
                    assertEquals(3, (int) da4.getMap().get("Three"));
                } else if (da instanceof TestDA1) {
                    da1Found = true;
                }
            }
            assertTrue(da1Found);
            assertTrue(da2Found);
            assertTrue(da3Found);
            assertTrue(da4Found);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    URL getXmlUrl(String xmlName) {
        return getResourceUrl("parser/" + xmlName);
    }

    URL getResourceUrl(String resourceName) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        assertNotNull("URL not null for: " + resourceName, url);
        return url;
    }
}
