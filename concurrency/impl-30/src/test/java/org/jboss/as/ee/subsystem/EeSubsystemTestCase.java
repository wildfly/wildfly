/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import java.io.IOException;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Eduardo Martins
 */
public class EeSubsystemTestCase extends AbstractSubsystemBaseTest {

    public EeSubsystemTestCase() {
        super(EeExtension.SUBSYSTEM_NAME, new EeExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-ee_6_0.xsd";
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(PathManager.SERVICE_DESCRIPTOR.getName());
    }
}
