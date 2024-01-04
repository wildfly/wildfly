/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.security;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;

public class SecurityDomainModelv11UnitTestCase extends AbstractSubsystemBaseTest {

    public SecurityDomainModelv11UnitTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("securitysubsystemv11.xml");
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-security_1_1.xsd";
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties p = new Properties();
        p.setProperty("jboss.server.config.dir", "/some/path");
        return p;
    }

    @Test
    public void testParseAndMarshalModelWithJASPI() throws Exception {
        super.standardSubsystemTest("securitysubsystemJASPIv11.xml", false);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }


}