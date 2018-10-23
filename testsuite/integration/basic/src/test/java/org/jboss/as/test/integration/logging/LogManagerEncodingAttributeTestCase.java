/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.logging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test checks if the LogManager won't throw "java.io.IOException: Stream Closed" or any other error after
 * setting the "encoding" attribute on the file handler (file-handler, periodic-rotating-file-handler,
 * size-rotating-file-handler and periodic-size-rotating-file-handler).
 *
 * Automated test for [ WFLY-8946 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(LogManagerEncodingAttributeServerSetupTask.class)
@RunAsClient
public class LogManagerEncodingAttributeTestCase {

    private static final String DEPLOYMENT = "deployment";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClass(LogManagerEncodingAttributeTestCase.class);
        return war;
    }

    @Test
    public void testLogManagerEncodingAttribute() {
    }

    @AfterClass
    public static void after() {
        try {
            System.setOut(LogManagerEncodingAttributeServerSetupTask.oldOut);
            String output = new String(LogManagerEncodingAttributeServerSetupTask.baos.toByteArray());
            Assert.assertFalse(output, output.contains("ERROR"));
            Assert.assertFalse(output, output.contains("java.io.IOException: Stream Closed"));
        } finally {
            System.setOut(LogManagerEncodingAttributeServerSetupTask.oldOut);
        }
    }
}
