/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsr77.subsystem;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JSR77ManagementSubsystemTestCase extends AbstractSubsystemBaseTest {

    public JSR77ManagementSubsystemTestCase() {
        super(JSR77ManagementExtension.SUBSYSTEM_NAME, new JSR77ManagementExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"urn:jboss:domain:jsr77:1.0\"/>";
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jsr77_1_0.xsd";
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }
}
