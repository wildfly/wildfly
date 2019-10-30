/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.metrics;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Resource adapters configuration and metrics unit test.
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RaCfgMetricUnitTestCase extends JCAMetrictsTestBase {

    public static void setBaseAddress(String rar) {
        baseAddress = new ModelNode();
        baseAddress.add("subsystem", "resource-adapters");
        baseAddress.add("resource-adapter", rar);
    }

    // @After - called after each test
    private void removeRa() throws Exception {
        removeDs();
    }

    // @Before - called from each test
    /*
     * Load data source model, stored in specified file to the configuration
     */
    protected void setModel(String modelName) throws Exception {
        setBaseAddress(modelName + ".rar");
        String xml = FileUtils.readFile(RaCfgMetricUnitTestCase.class, "ra/" + modelName + ".xml");
        List<ModelNode> operations = xmlToModelOperations(xml, Namespace.RESOURCEADAPTERS_1_0.getUriString(),
                new ResourceAdapterSubsystemParser());
        executeOperation(operationListToCompositeOperation(operations));
    }


    @Test
    public void testAttributes() throws Exception {
        setModel("simple");
        ModelNode address1 = baseAddress.clone();
        address1.add("connection-definitions", "name1");
        address1.protect();
        assertTrue(readAttribute(address1, "use-ccm").asBoolean());
        assertTrue(readAttribute(address1, "use-java-context").asBoolean());
        ModelNode address2 = baseAddress.clone();
        address2.add("admin-objects", "Name3");
        address2.protect();
        assertTrue(readAttribute(address2, "use-java-context").asBoolean());
        removeRa();
    }

    @Test
    public void testEmpty() throws Exception {
        setModel("empty");
        assertEquals("empty.rar", readAttribute(baseAddress, "archive").asString());
        removeRa();
    }

    @Test(expected = Exception.class)
    public void test2Archives() throws Exception {
        setBadModel("wrong-2-archives");
    }

    @Test(expected = Exception.class)
    public void testNoArchives() throws Exception {
        setBadModel("wrong-no-archives");
    }

    @Test
    public void test2DiffConfProp() throws Exception {
        setModel("2-diff-conf-prop");
        ModelNode address1 = baseAddress.clone();
        address1.add("config-properties", "name1");
        address1.protect();
        assertEquals("value1", readAttribute(address1, "value").asString());
        ModelNode address2 = baseAddress.clone();
        address2.add("config-properties", "name2");
        address2.protect();
        assertEquals("value2", readAttribute(address2, "value").asString());
        removeRa();
    }

    @Test(expected = Exception.class)
    public void testConfPropWithEqualNames() throws Exception {
        setBadModel("wrong-equal-conf-prop");
    }

    // no excepton sent because it's like omit the tag
    @Test
    public void testEmptyTrans() throws Exception {
        setBadModel("wrong-empty-trans");
    }

    @Test(expected = Exception.class)
    public void testWrongTrans() throws Exception {
        setBadModel("wrong-trans");
    }

    @Test(expected = Exception.class)
    public void testWrong2Trans() throws Exception {
        setBadModel("wrong-2-trans");
    }

    @Test(expected = Exception.class)
    public void testWrongXAPoolWithTrans() throws Exception {
        setBadModel("wrong-xa-pool-trans");
    }

    @Test(expected = Exception.class)
    public void testWrongPoolWithXATrans() throws Exception {
        setBadModel("wrong-pool-xa-trans");
    }

    @Test(expected = Exception.class)
    public void test2Pools() throws Exception {
        setBadModel("wrong-2-pools");
    }

    @Test(expected = Exception.class)
    public void test2XaPools() throws Exception {
        setBadModel("wrong-2-xa-pools");
    }

    @Test(expected = Exception.class)
    public void testPoolAndXaPool() throws Exception {
        setBadModel("wrong-pool-and-xa-pool");
    }

    @Test(expected = Exception.class)
    public void testMinPoolSize() throws Exception {
        setBadModel("wrong-min-pool-size");
    }

    @Test(expected = Exception.class)
    public void testMaxPoolSize() throws Exception {
        setBadModel("wrong-max-pool-size");
    }

    @Test(expected = Exception.class)
    public void testMinGtMaxPoolSize() throws Exception {
        setBadModel("wrong-min-max-pool-size");
    }

    @Test
    public void testBoolPresProperties() throws Exception {
        setModel("bool-pres-properties");
        ModelNode address1 = baseAddress.clone();
        address1.add("connection-definitions", "name1");
        address1.protect();
        assertTrue(readAttribute(address1, "interleaving").asBoolean());
        assertTrue(readAttribute(address1, "no-tx-separate-pool").asBoolean());
        assertTrue(readAttribute(address1, "security-application").asBoolean());
        assertTrue(readAttribute(address1, "wrap-xa-resource").asBoolean());
        assertFalse(readAttribute(address1, "pad-xid").asBoolean());
        assertFalse(readAttribute(address1, "same-rm-override").isDefined());
        try {
            readAttribute(address1, "same-rm-override").asBoolean();
            fail("Got  boolean value of undefined parameter");
        } catch (Exception e) {
            //Expected
        } finally {
            removeRa();
        }
    }

    @Test
    public void testBoolPresPropertiesSet() throws Exception {
        setModel("bool-pres-properties-set");
        ModelNode address1 = baseAddress.clone();
        address1.add("connection-definitions", "name1");
        address1.protect();
        assertTrue(readAttribute(address1, "interleaving").asBoolean());
        assertTrue(readAttribute(address1, "no-tx-separate-pool").asBoolean());
        assertTrue(readAttribute(address1, "security-application").asBoolean());
        assertTrue(readAttribute(address1, "no-recovery").asBoolean());

        removeRa();
    }

    @Test
    public void testBoolPresPropertiesUnset() throws Exception {
        setModel("bool-pres-properties-unset");
        ModelNode address1 = baseAddress.clone();
        address1.add("connection-definitions", "name1");
        address1.protect();
        assertFalse(readAttribute(address1, "interleaving").asBoolean());
        assertFalse(readAttribute(address1, "no-tx-separate-pool").asBoolean());
        assertFalse(readAttribute(address1, "no-recovery").asBoolean());

        removeRa();
    }

    @Test(expected = Exception.class)
    public void testSecurity1() throws Exception {
        setBadModel("wrong-security-1");
    }

    @Test(expected = Exception.class)
    public void testSecurity2() throws Exception {
        setBadModel("wrong-security-2");
    }

    @Test(expected = Exception.class)
    public void testSecurity3() throws Exception {
        setBadModel("wrong-security-3");
    }

    @Test(expected = Exception.class)
    public void testSecurity4() throws Exception {
        setBadModel("wrong-security-4");
    }

    @Test(expected = Exception.class)
    public void test2SecurityDomains() throws Exception {
        setBadModel("wrong-2-security-domains");
    }

    @Test(expected = Exception.class)
    public void testFlushStrategy() throws Exception {
        setBadModel("wrong-flush-strategy");
    }

    @Test(expected = Exception.class)
    public void testNegBlckgTmt() throws Exception {
        setBadModel("wrong-blck-tmt");
    }

    @Test(expected = Exception.class)
    public void testNegIdleTmt() throws Exception {
        setBadModel("wrong-idle-tmt");
    }

    @Test(expected = Exception.class)
    public void testNegAllocRetry() throws Exception {
        setBadModel("wrong-alloc-retry");
    }

    @Test(expected = Exception.class)
    public void testNegAllocRetryWait() throws Exception {
        setBadModel("wrong-alloc-retry-wait");
    }

    @Test(expected = Exception.class)
    public void testNegXaResTmt() throws Exception {
        setBadModel("wrong-xa-res-tmt");
    }

    @Test(expected = Exception.class)
    public void testNegBckgValid() throws Exception {
        setBadModel("wrong-bckg-tmt");
    }

    @Test(expected = Exception.class)
    public void testRecoverPlugin() throws Exception {
        setBadModel("wrong-recover-plugin");
    }

    @Test(expected = Exception.class)
    public void testConDefWoClass() throws Exception {
        setBadModel("wrong-2-con-def-wo-class");
    }

    @Test(expected = Exception.class)
    public void testAoWoClass() throws Exception {
        setBadModel("wrong-2-ao-wo-class");
    }
}
