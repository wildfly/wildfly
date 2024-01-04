/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.metrics;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class Subsystem_1_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_1_0_ParsingTestCase() {
        super(MetricsExtension.SUBSYSTEM_NAME, new MetricsExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_1_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-metrics_1_0.xsd";
    }
}