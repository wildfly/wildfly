/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxb.unit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>Test for JAXB using a System Property. The test will try using the
 * default implementation inside the module.</p>
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@ServerSetup({JAXBContextSystemPropInternalTestCase.SystemPropertiesSetup.class})
@RunAsClient
public class JAXBContextSystemPropInternalTestCase extends JAXBContextTestBase {

   /**
     * Setup the system property to configure internal jaxb implementation.
     */
    static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {

        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[] {
                new DefaultSystemProperty(JAVAX_FACTORY_PROP_NAME, JAVAX_JAXB_FACTORY_CLASS),
                new DefaultSystemProperty(JAKARTA_FACTORY_PROP_NAME, JAKARTA_JAXB_FACTORY_CLASS)
            };
        }
    }

    @Deployment(name = "app-internal", testable = false)
    public static WebArchive createInternalDeployment() {
        return JAXBContextTestBase.createInternalDeployment();
    }

    @OperateOnDeployment("app-internal")
    @Test
    public void testInternal() throws Exception {
        testDeafultImplementation(url);
    }
}
