/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.jaspi;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Test case testing a deployment secured using JASPI configured within the Elytron subsystem with the actual authentication
 * handled by the mapped SecurityDomain of the deployment.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ ConfiguredJaspiTestCase.ServerSetup.class })
public class ConfiguredJaspiTestCase extends ConfiguredJaspiTestBase {

    private static final String NAME = ConfiguredJaspiTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createDeployment(NAME);
    }

    static class ServerSetup extends ConfiguredJaspiTestBase.ServerSetup {

        @Override
        protected String getName() {
            return NAME;
        }

        @Override
        protected boolean enableAnonymousLogin() {
            return true;
        }

    }
}
