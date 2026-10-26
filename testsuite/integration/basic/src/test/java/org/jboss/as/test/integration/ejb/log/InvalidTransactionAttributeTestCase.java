/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.log;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;


/**
 * Tests if the WARN message was logged when default transaction attribute was used on SFSB lifecyle method
 * Test for [ WFLY-9509 ].
 *
 * @author Daniel Cihak
 */
@ExtendWith(ArquillianExtension.class)
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
                String output = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                Assertions.assertFalse(output.contains("WFLYEJB0463"), output);
                Assertions.assertFalse(output.contains("ERROR"), output);
            } finally {
                deployer.undeploy("invalidtransactionattribute");
            }
        } finally {
            System.setOut(oldOut);
        }
    }
}
