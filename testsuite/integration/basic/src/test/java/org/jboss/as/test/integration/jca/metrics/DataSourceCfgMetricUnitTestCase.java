/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Datasource configuration and metrics unit test.
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class DataSourceCfgMetricUnitTestCase extends JCAMetricsTestBase {

    @BeforeAll
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

    @Test
    public void testDsWithNoDriver() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-no-driver.xml"));
    }

    @Test
    public void testDsWithWrongTransactionIsolationType() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-transaction-isolation-type.xml"));
    }

    @Test
    public void testValidationDefaultProperties() throws Exception {
        setModel("validation-properties.xml");
        assertFalse(readAttribute(baseAddress, "use-fast-fail").asBoolean());
        removeDs();
    }

    @Test
    public void testWrongValidationProperties() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-validation-properties.xml"));
    }

    @Test
    public void testTimeoutDefaultProperties() throws Exception {
        setModel("timeout-properties.xml");
        assertFalse(readAttribute(baseAddress, "set-tx-query-timeout").asBoolean());
        removeDs();
    }

    @Test
    public void testWrongBlckgTimeoutProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-blckg-timeout-property.xml"));
    }

    @Test
    public void testWrongIdleMinsProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-idle-mins-property.xml"));
    }

    @Test
    public void testWrongAllocRetryProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-alloc-retry-property.xml"));
    }

    @Test
    public void testWrongAllocRetryWaitProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-alloc-retry-wait-property.xml"));
    }

    @Test
    public void testStatementDefaultProperties() throws Exception {
        setModel("statement-properties.xml");
        assertEquals("nowarn", readAttribute(baseAddress, "track-statements").asString());
        assertFalse(readAttribute(baseAddress, "share-prepared-statements").asBoolean());
        removeDs();
    }

    @Test
    public void testWrongTrckStmtProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-trck-stmt-property.xml"));
    }

    @Test
    public void testWrongStmtCacheSizeProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-stmt-cache-size-property.xml"));
    }

    @Test
    public void testWrongFlushStrategyProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-flush-strategy-property.xml"));
    }

    @Test
    public void testWrongMinPoolSizeProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-min-pool-size-property.xml"));
    }

    @Test
    public void testWrongMaxPoolSizeProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-max-pool-size-property.xml"));
    }

    @Test
    public void testWrongMaxLessMinPoolSizeProperty() {
        assertThrows(Exception.class, () ->
            setBadModel("wrong-max-less-min-pool-size-property.xml"));
    }

    @Test
    public void testStatistics() throws Exception {
        super.testStatistics("basic-attributes.xml");
    }
}
