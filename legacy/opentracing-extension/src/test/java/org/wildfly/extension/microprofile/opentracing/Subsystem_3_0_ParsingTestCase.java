/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.opentracing;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;

public class Subsystem_3_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_3_0_ParsingTestCase() {
        super(SubsystemExtension.SUBSYSTEM_NAME, new SubsystemExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_3_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-microprofile-opentracing_3_0.xsd";
    }

    @Override
    protected Properties getResolvedProperties() {
        return System.getProperties();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return OpentracingAdditionalInitialization.INSTANCE;
    }

}
