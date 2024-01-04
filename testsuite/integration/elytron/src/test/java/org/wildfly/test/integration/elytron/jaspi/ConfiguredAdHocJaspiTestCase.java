/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.jaspi;

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Test case testing a deployment secured using JASPI configured within the Elytron subsystem with the authentication being
 * handled by the ServerAuthModule and an AdHoc identity being created.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ ConfiguredAdHocJaspiTestCase.ServerSetup.class })
public class ConfiguredAdHocJaspiTestCase extends ConfiguredJaspiTestBase {

    private static final String NAME = ConfiguredAdHocJaspiTestCase.class.getSimpleName();

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
        protected String getMode() {
            // We still want self-validating as ad-hoc does not support PasswordValidationCallback
            return "self-validating";
        }

        @Override
        protected Map<String, String> getOptions() {
            Map<String, String> options = new HashMap<>();
            options.putAll(super.getOptions());
            options.put("default-roles", "Role1");
            return options;
        }

        @Override
        protected boolean isIntegratedJaspi() {
            return false;
        }



    }

}
