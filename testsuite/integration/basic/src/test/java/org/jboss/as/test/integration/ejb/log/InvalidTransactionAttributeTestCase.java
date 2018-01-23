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
    public void testInvalidTransactionAttributeWarnLogged() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));

        deployer.deploy("invalidtransactionattribute");

        System.setOut(oldOut);
        String output = new String(baos.toByteArray());

        Assert.assertFalse(output.contains("WFLYEJB0463"));

        deployer.undeploy("invalidtransactionattribute");
    }
}
