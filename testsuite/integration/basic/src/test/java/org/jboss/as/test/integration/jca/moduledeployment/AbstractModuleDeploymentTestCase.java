/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.moduledeployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Set;
import javax.naming.InitialContext;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;

import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * AS7-5768 -Support for RA module deployment
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public abstract class AbstractModuleDeploymentTestCase extends
        ContainerResourceMgmtTestBase {


    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    public static JavaArchive createDeployment(boolean withDependencies) throws Exception {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addClasses(MgmtOperationException.class, XMLElementReader.class,
                XMLElementWriter.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage())
                .addPackage(AbstractModuleDeploymentTestCase.class.getPackage());

        if (withDependencies) {
            ja.addAsManifestResource(
                    new StringAsset(
                            "Dependencies: org.jboss.as.controller-client,org.jboss.dmr,javax.inject.api,org.jboss.as.connector,org.wildfly.common\n"),
                    "MANIFEST.MF");
        }

        return ja;

    }

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    public static JavaArchive createDeployment() throws Exception {
        return createDeployment(true);
    }

    /**
     * Test configuration
     *
     * @throws Throwable in case of an error
     */
    public void testConnectionFactory(ConnectionFactory connectionFactory)
            throws Throwable {
        assertNotNull(connectionFactory);
        Connection c = connectionFactory.getConnection();
        assertNotNull(c);
        c.close();
    }

    /**
     * Tests connection in pool
     *
     * @throws Exception in case of error
     */
    public void testConnection(String conName) throws Exception {
        final ModelNode address1 = getAddress().clone();
        address1.add("connection-definitions", conName);
        address1.protect();

        final ModelNode operation1 = new ModelNode();
        operation1.get(OP).set("test-connection-in-pool");
        operation1.get(OP_ADDR).set(address1);
        executeOperation(operation1);
    }

    /**
     * Returns basic address of resource adapter
     *
     * @return address
     */
    protected abstract ModelNode getAddress();

    /**
     * Finding object by JNDI name and checks, if its String representation
     * contains expected substrings
     *
     * @param jndiName of object
     * @param contains - substring, must be contained
     * @throws Exception
     */
    public void testJndiObject(String jndiName, String... contains)
            throws Exception {
        Object o = new InitialContext().lookup(jndiName);
        assertNotNull(o);
        for (String c : contains) {
            assertTrue(o.toString() + " should contain " + c, o.toString()
                    .contains(c));
        }

    }

    /**
     * Checks Set if there is a String element, containing some substring and
     * returns it
     *
     * @param ids     - Set
     * @param contain - substring
     * @return String
     */
    public String getElementContaining(Set<String> ids, String contain) {
        Iterator<String> it = ids.iterator();
        while (it.hasNext()) {
            String t = it.next();
            if (t.contains(contain)) {
                return t;
            }
        }
        return null;
    }

}
