/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.jaxb.unit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>Test for JAXB using a <em>jaxb.properties</em> file. The test will try to
 * use the default implementation inside the module and a custom/fake one.</p>
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JAXBContextUsingPropertiesTestCase extends JAXBContextTestBase {

    @Deployment(name = "app-internal", testable = false)
    public static WebArchive createInternalDeployment() {
        final WebArchive war = JAXBContextTestBase.createInternalDeployment();
        war.add(new StringAsset(JAXB_FACTORY_PROP_NAME + "=" + DEFAULT_JAXB_FACTORY_CLASS), JAXB_PROPERTIES_FILE);
        return war;
    }

    @Deployment(name = "app-custom", testable = false)
    public static WebArchive createCustomDeployment() {
        final WebArchive war = JAXBContextTestBase.createCustomDeployment();
        war.add(new StringAsset(JAXB_FACTORY_PROP_NAME + "=" + CUSTOM_JAXB_FACTORY_CLASS), JAXB_PROPERTIES_FILE);
        return war;
    }

    @OperateOnDeployment("app-internal")
    @Test
    public void testInternal() throws Exception {
        testDeafultImplementation(url);
    }

    @OperateOnDeployment("app-custom")
    @Test
    public void testCustom() throws Exception {
        testCustomImplementation(url);
    }
}
