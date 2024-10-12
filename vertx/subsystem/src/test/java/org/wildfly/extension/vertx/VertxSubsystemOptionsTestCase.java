/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Test;

/**
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemOptionsTestCase extends AbstractSubsystemBaseTest {

    public VertxSubsystemOptionsTestCase() {
        super(VertxSubsystemExtension.SUBSYSTEM_NAME, new VertxSubsystemExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("vertx-options-full.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-vertx_1_0_0.xsd";
    }

    @Test
    @Override
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null, false);
    }

}
