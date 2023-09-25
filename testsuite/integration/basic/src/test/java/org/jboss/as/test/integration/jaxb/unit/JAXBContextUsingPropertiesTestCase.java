/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxb.unit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
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
        String nl = System.getProperty("line.separator");
        war.add(new StringAsset(
                JAVAX_FACTORY_PROP_NAME + "=" + JAVAX_JAXB_FACTORY_CLASS + nl
                + JAKARTA_FACTORY_PROP_NAME + "=" + JAKARTA_JAXB_FACTORY_CLASS),
                JAXB_PROPERTIES_FILE);
        return war;
    }

    @Deployment(name = "app-custom", testable = false)
    public static WebArchive createCustomDeployment() {
        final WebArchive war = JAXBContextTestBase.createCustomDeployment();
        String nl = System.getProperty("line.separator");
        war.add(new StringAsset(
                JAVAX_FACTORY_PROP_NAME + "=" + CUSTOM_JAXB_FACTORY_CLASS + nl
                + JAKARTA_FACTORY_PROP_NAME + "=" + CUSTOM_JAXB_FACTORY_CLASS),
                JAXB_PROPERTIES_FILE);
        return war;
    }

    @OperateOnDeployment("app-internal")
    @Test
    public void testInternal() throws Exception {
        testDeafultImplementation(url);
    }

    @OperateOnDeployment("app-custom")
    @Test
    @Ignore("WFLY-16523")
    public void testCustom() throws Exception {
        testCustomImplementation(url);
    }
}
