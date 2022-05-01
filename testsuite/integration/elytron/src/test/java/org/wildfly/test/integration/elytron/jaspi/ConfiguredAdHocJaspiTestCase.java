/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.elytron.jaspi;

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.categories.RequiresTransformedClass;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.experimental.categories.Category;
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
@Category(RequiresTransformedClass.class)
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
