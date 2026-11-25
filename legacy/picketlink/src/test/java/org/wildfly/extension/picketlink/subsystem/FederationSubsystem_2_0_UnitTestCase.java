/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.subsystem;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;
import org.wildfly.extension.picketlink.federation.FederationExtension;

/**
 * @author Pedro Igor
 */
public class FederationSubsystem_2_0_UnitTestCase extends AbstractSubsystemBaseTest {

    public FederationSubsystem_2_0_UnitTestCase() {
        super(FederationExtension.SUBSYSTEM_NAME, new FederationExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("federation-subsystem-2.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-picketlink-federation_2_0.xsd";
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("federation-subsystem-expressions-2.0.xml");
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.ADMIN_ONLY_HC;
    }

}
