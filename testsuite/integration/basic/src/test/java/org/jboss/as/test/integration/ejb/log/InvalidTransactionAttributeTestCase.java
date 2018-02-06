/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.log;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


/**
 * Tests if the WARN message was logged when default transaction attribute was used on SFSB lifecyle method
 * Test for [ WFLY-9509 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
public class InvalidTransactionAttributeTestCase {

    private static final String ARCHIVE_NAME = "InvalidTransactionAttribute";

    @ArquillianResource
    Deployer deployer;

    @Deployment(name = "invalidtransactionattribute", testable = false, managed = false)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(InvalidTransactionAttributeTestCase.class, StatefulBean.class, StatefulInterface.class);
        jar.addAsManifestResource(InvalidTransactionAttributeTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testInvalidTransactionAttributeWarnLogged() {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(baos));
            deployer.deploy("invalidtransactionattribute");
            try {
                System.setOut(oldOut);
                String output = new String(baos.toByteArray());
                Assert.assertFalse(output, output.contains("WFLYEJB0463"));
                Assert.assertFalse(output, output.contains("ERROR"));
            } finally {
                deployer.undeploy("invalidtransactionattribute");
            }
        } finally {
            System.setOut(oldOut);
        }
    }
}
