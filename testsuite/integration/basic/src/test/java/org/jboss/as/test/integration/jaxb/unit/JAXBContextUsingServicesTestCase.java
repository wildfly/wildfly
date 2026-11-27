/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxb.unit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * <p>Test for JAXB using a <em>META-INF/services/</em> file. The test will try
 * to use the custom/fake implementation provided by the app.</p>
 *
 * @author rmartinc
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@Disabled("WFLY-16523")
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
