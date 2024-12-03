/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JaxrsSubsystemTestCase extends AbstractSubsystemBaseTest {

    public JaxrsSubsystemTestCase() {
        super(JaxrsExtension.SUBSYSTEM_NAME, new JaxrsExtension());
    }

    @Override
    protected String getSubsystemXml() {
        return "<subsystem xmlns=\"urn:jboss:domain:jaxrs:1.0\"/>";
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/jboss-as-jaxrs_1_0.xsd";
    }

    @Test
    @Override
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null, false);
    }
}
