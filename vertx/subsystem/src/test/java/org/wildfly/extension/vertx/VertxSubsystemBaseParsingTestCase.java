/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
