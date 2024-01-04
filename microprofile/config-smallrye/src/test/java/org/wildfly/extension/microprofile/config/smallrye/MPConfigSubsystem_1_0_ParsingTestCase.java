/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;

public class MPConfigSubsystem_1_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public MPConfigSubsystem_1_0_ParsingTestCase() {
        super(MicroProfileConfigExtension.SUBSYSTEM_NAME, new MicroProfileConfigExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_1_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-microprofile-config-smallrye_1_0.xsd";
    }

    protected Properties getResolvedProperties() {
        return System.getProperties();
    }


    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                WELD_CAPABILITY_NAME);
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }
}
