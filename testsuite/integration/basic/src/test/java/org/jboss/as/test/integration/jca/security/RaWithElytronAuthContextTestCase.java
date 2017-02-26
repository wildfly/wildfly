/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.jca.security;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.resource.cci.Connection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerSetupTask;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.MatchRules;
import org.wildfly.test.security.common.elytron.SimpleAuthConfig;
import org.wildfly.test.security.common.elytron.SimpleAuthContext;

/**
 * Test for RA with elytron security domain
 *
 * @author Flavia Rainone
 */
@RunWith(Arquillian.class)
@ServerSetup(RaWithElytronAuthContextTestCase.ElytronSetup.class)
public class RaWithElytronAuthContextTestCase {

    private static final String AUTH_CONFIG = "MyAuthConfig";
    private static final String AUTH_CONTEXT = "MyAuthContext";
    private static final String CREDENTIAL = "sa";

    static class ElytronSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            final CredentialReference credRefPwd = CredentialReference.builder().withClearText(CREDENTIAL).build();
            final ConfigurableElement authenticationConfiguration = SimpleAuthConfig.builder().withName(AUTH_CONFIG)
                    .withAuthenticationName(CREDENTIAL).withCredentialReference(credRefPwd).build();
            final MatchRules matchRules = MatchRules.builder().withAuthenticationConfiguration(AUTH_CONFIG).build();
            final ConfigurableElement authenticationContext = SimpleAuthContext.builder().withName(AUTH_CONTEXT).
                    withMatchRules(matchRules).build();

            return new ConfigurableElement[] {authenticationConfiguration, authenticationContext};
        }
    }

    @Deployment
    public static Archive<?> deploymentSingleton() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar")
                .addClass(RaWithElytronAuthContextTestCase.class)
                .addPackage(MultipleConnectionFactory1.class.getPackage());
        jar.addClasses(AbstractElytronSetupTask.class, ServerSetupTask.class);
        final ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "test.rar")
                .addAsLibrary(jar)
                .addAsManifestResource(RaWithElytronAuthContextTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(RaWithElytronAuthContextTestCase.class.getPackage(), "ironjacamar-elytron.xml",
                        "ironjacamar.xml");

        return rar;
    }

    @Resource(mappedName = "java:jboss/name1")
    private MultipleConnectionFactory1 connectionFactory1;

    @Test
    public void deploymentTest() throws Exception {
        assertNotNull("CF1 not found", connectionFactory1);
        Connection cci = connectionFactory1.getConnection();
        assertNotNull("Cannot obtain connection", cci);
        cci.close();
    }
}
