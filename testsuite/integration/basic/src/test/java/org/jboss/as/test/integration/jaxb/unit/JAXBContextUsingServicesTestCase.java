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
 * <p>Test for JAXB using a <em>META-INF/services/</em> file. The test will try
 * to use the custom/fake implementation provided by the app.</p>
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JAXBContextUsingServicesTestCase extends JAXBContextTestBase {

    @Deployment(name = "app-custom", testable = false)
    public static WebArchive createCustomDeployment() {
        final WebArchive war = JAXBContextTestBase.createCustomDeployment();
        war.add(new StringAsset(CUSTOM_JAXB_FACTORY_CLASS), SERVICES_FILE);
        return war;
    }

    @OperateOnDeployment("app-custom")
    @Test
    public void testCustom() throws Exception {
        testCustomImplementation(url);
    }
}
