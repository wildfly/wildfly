/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.datasources;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.version.Stability;
import org.junit.Test;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="stefano.maestri@redhat.com>Stefano Maestri</a>
 */
public class DatasourcesSubsystemTestCase extends AbstractSubsystemBaseTest {

    public DatasourcesSubsystemTestCase() {
        super(DataSourcesExtension.SUBSYSTEM_NAME, new DataSourcesExtension(), Stability.COMMUNITY);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        // The validation-timeout-seconds attribute is not available when running at default stability
        return new AdditionalInitialization.ManagementAdditionalInitialization(Stability.COMMUNITY);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //test configuration put in standalone.xml
        return readResource("datasources-full.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-datasources_community_8_0.xsd";
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("datasources-full.xml");
    }

    @Test
    public void testFullConfig7_0() throws Exception {
        standardSubsystemTest("datasources-full-7.0.xml", false);
    }

    @Test
    public void testFullConfig6_0() throws Exception {
        standardSubsystemTest("datasources-full-6.0.xml", false);
    }

    @Test
    public void testElytronConfig() throws Exception {
        standardSubsystemTest("datasources-elytron-enabled.xml");
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("datasources-full-expression.xml", "datasources-full.xml");
    }

    @Test
    public void testElytronExpressionConfig() throws Exception {
        standardSubsystemTest("datasources-elytron-enabled-expression.xml", "datasources-elytron-enabled.xml");
    }
}

