/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:ema@rehdat.com>Jim Ma</a>
 * @author <a href="mailto:alessio.soldano@jboss.com>Alessio Soldano</a>
 * @author <a href="rsigal@redhat.com>Ron Sigal</a>
 */
public class JaxrsSubsystem20TestCase extends AbstractSubsystemBaseTest {

    public JaxrsSubsystem20TestCase() {
        super(JaxrsExtension.SUBSYSTEM_NAME, new JaxrsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jaxrs-2.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/jboss-as-jaxrs_2_0.xsd";
    }

    @Override
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null, false);
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("jaxrs-2.0-expressions.xml", false);
    }
}
