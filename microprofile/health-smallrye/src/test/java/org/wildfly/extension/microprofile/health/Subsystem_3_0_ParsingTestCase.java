/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;

import java.io.IOException;
import java.util.Properties;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.HEALTH_HTTP_CONTEXT_CAPABILITY;
import static org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition.HEALTH_SERVER_PROBE_CAPABILITY;

/**
 * @author <a href="http://xstefank.io/">Martin Stefanko</a> (c) 2021 Red Hat inc.
 */
public class Subsystem_3_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_3_0_ParsingTestCase() {
        super(MicroProfileHealthExtension.SUBSYSTEM_NAME, new MicroProfileHealthExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_3_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-microprofile-health-smallrye_3_0.xsd";
    }

    protected Properties getResolvedProperties() {
        return System.getProperties();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                WELD_CAPABILITY_NAME,
                "org.wildfly.management.executor",
                "org.wildfly.management.http.extensible",
                HEALTH_HTTP_CONTEXT_CAPABILITY,
                HEALTH_SERVER_PROBE_CAPABILITY);
    }
}
