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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Datasource configuration and metrics unit test.
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourceCfgMetricUnitTestCase extends JCAMetrictsTestBase {

    @BeforeClass
    public static void before() {
        setBaseAddress("data-source", "DS");
    }

    @Test
    public void testDefaultDsAttributes() throws Exception {
        setModel("basic-attributes.xml");
        assertTrue(readAttribute(baseAddress, "use-ccm").asBoolean());
        assertTrue(readAttribute(baseAddress, "jta").asBoolean());
        assertTrue(readAttribute(baseAddress, "use-java-context").asBoolean());
        assertFalse(readAttribute(baseAddress, "spy").asBoolean());
        removeDs();
    }

    @Test(expected = Exception.class)
    public void testDsWithNoDriver() throws Exception {
        setBadModel("wrong-no-driver.xml");
    }

    @Test(expected = Exception.class)
    public void testDsWithWrongTransactionIsolationType() throws Exception {
        setBadModel("wrong-transaction-isolation-type.xml");
    }

    @Test
    public void testValidationDefaultProperties() throws Exception {
        setModel("validation-properties.xml");
        assertFalse(readAttribute(baseAddress, "use-fast-fail").asBoolean());
        removeDs();
    }

    @Test(expected = Exception.class)
    public void testWrongValidationProperties() throws Exception {
        setBadModel("wrong-validation-properties.xml");
    }

    @Test
    public void testTimeoutDefaultProperties() throws Exception {
        setModel("timeout-properties.xml");
        assertFalse(readAttribute(baseAddress, "set-tx-query-timeout").asBoolean());
        removeDs();
    }

    @Test(expected = Exception.class)
    public void testWrongBlckgTimeoutProperty() throws Exception {
        setBadModel("wrong-blckg-timeout-property.xml");
    }

    @Test(expected = Exception.class)
    public void testWrongIdleMinsProperty() throws Exception {
        setBadModel("wrong-idle-mins-property.xml");
    }

    @Test(expected = Exception.class)
    public void testWrongAllocRetryProperty() throws Exception {
        setBadModel("wrong-alloc-retry-property.xml");
    }

    @Test(expected = Exception.class)
    public void testWrongAllocRetryWaitProperty() throws Exception {
        setBadModel("wrong-alloc-retry-wait-property.xml");
    }

    @Test
    public void testStatementDefaultProperties() throws Exception {
        setModel("statement-properties.xml");
        assertEquals("NOWARN", readAttribute(baseAddress, "track-statements").asString());
        assertFalse(readAttribute(baseAddress, "share-prepared-statements").asBoolean());
        removeDs();
    }

    @Test(expected = Exception.class)
    public void testWrongTrckStmtProperty() throws Exception {
        setBadModel("wrong-trck-stmt-property.xml");
    }

    @Test(expected = Exception.class)
    public void testWrongStmtCacheSizeProperty() throws Exception {
        setBadModel("wrong-stmt-cache-size-property.xml");
    }

    @Test(expected = Exception.class)
    public void testWrongFlushStrategyProperty() throws Exception {
        setBadModel("wrong-flush-strategy-property.xml");
    }

    @Test(expected = Exception.class)
    public void testWrongMinPoolSizeProperty() throws Exception {
        setBadModel("wrong-min-pool-size-property.xml");
    }

    @Test(expected = Exception.class)
    public void testWrongMaxPoolSizeProperty() throws Exception {
        setBadModel("wrong-max-pool-size-property.xml");
    }

    @Test(expected = Exception.class)
    public void testWrongMaxLessMinPoolSizeProperty() throws Exception {
        setBadModel("wrong-max-less-min-pool-size-property.xml");
    }

    @Test
    public void testStatistics() throws Exception {
        super.testStatistics("basic-attributes.xml");
    }
}
