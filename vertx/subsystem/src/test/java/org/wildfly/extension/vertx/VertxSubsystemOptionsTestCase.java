/*
 * Copyright (C) 2022 RedHat
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
        return "schema/wildfly-vertx_1_0.xsd";
    }

    @Test
    @Override
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null, false);
    }

}
