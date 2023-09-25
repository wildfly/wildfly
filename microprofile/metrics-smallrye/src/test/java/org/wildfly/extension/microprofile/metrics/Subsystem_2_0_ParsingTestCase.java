/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.metrics;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class Subsystem_2_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_2_0_ParsingTestCase() {
        super(MicroProfileMetricsExtension.SUBSYSTEM_NAME, new MicroProfileMetricsExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_2_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-microprofile-metrics-smallrye_2_0.xsd";
    }

    @Override
    protected Properties getResolvedProperties() {
        return System.getProperties();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }

}
