/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Test;

import java.io.IOException;

/**
 * This is the bare-bones test example that tests subsystem
 * It does same things that {@link VertxSubsystemParsingTestCase} does but most of internals are already done in AbstractSubsystemBaseTest
 * If you need more control over what happens in tests look at  {@link VertxSubsystemParsingTestCase}
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public VertxSubsystemBaseParsingTestCase() {
        super(VertxSubsystemExtension.SUBSYSTEM_NAME, new VertxSubsystemExtension());
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-vertx_1_0_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/vertx.xml"
        };
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"" + VertxSubsystemParser_1_0.NAMESPACE + "\">" +
                "</subsystem>";
    }

    @Test
    @Override
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null, false);
    }

}
