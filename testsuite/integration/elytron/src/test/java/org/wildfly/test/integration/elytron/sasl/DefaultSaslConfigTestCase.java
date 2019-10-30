/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.sasl;

import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.wildfly.test.integration.elytron.sasl.AbstractSaslTestBase.JmsSetup;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantRoleMapper;
import org.wildfly.test.security.common.other.SimpleRemotingConnector;
import org.wildfly.test.security.common.other.SimpleSocketBinding;

/**
 * Elytron SASL mechanisms tests which use Naming + JMS client. The server setup adds for each tested SASL configuration a new
 * native remoting port and client tests functionality against it.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ JmsSetup.class, DefaultSaslConfigTestCase.ServerSetup.class })
public class DefaultSaslConfigTestCase extends AbstractSaslTestBase {

    private static final String DEFAULT_SASL_AUTHENTICATION = "application-sasl-authentication";
    private static final String DEFAULT = "DEFAULT";
    private static final int PORT_DEFAULT = 10568;

    /**
     * Tests that ANONYMOUS SASL mechanism can't be used for authentication in default server configuration.
     */
    @Test
    public void testAnonymousFailsInDefault() throws Exception {
        // Anonymous not supported in the default configuration
        AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty()
                                .setSaslMechanismSelector(SaslMechanismSelector.fromString("ANONYMOUS")).useAnonymous())
                .run(() -> sendAndReceiveMsg(PORT_DEFAULT, true));
    }

    /**
     * Tests that JBOSS-LOCAL-USER SASL mechanism can be used for authentication in default server configuration.
     */
    @Test
    @Ignore("WFLY-8961")
    public void testJBossLocalInDefault() throws Exception {
        AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty()
                                .setSaslMechanismSelector(SaslMechanismSelector.fromString("JBOSS-LOCAL-USER")))
                .run(() -> sendAndReceiveMsg(PORT_DEFAULT, false));
    }

    /**
     * Tests that DIGEST-MD5 SASL mechanism can be used for authentication in default server configuration.
     */
    @Test
    public void testDigestInDefault() throws Exception {
        AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty()
                                .setSaslMechanismSelector(SaslMechanismSelector.fromString("DIGEST-MD5")).useName("guest")
                                .usePassword("guest"))
                .run(() -> sendAndReceiveMsg(PORT_DEFAULT, false, "guest", "guest"));
    }

    /**
     * Setup task which configures remoting connectors for this test.
     */
    public static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            // let all the authenticated users has the guest role
            elements.add(ConstantRoleMapper.builder().withName("guest").withRoles("guest").build());
            elements.add(new ConfigurableElement() {

                @Override
                public void create(CLIWrapper cli) throws Exception {
                    cli.sendLine("/subsystem=elytron/security-domain=ApplicationDomain:write-attribute(name=role-mapper, value=guest)");
                }

                @Override
                public void remove(CLIWrapper cli) throws Exception {
                    cli.sendLine("/subsystem=elytron/security-domain=ApplicationDomain:write-attribute(name=role-mapper)");
                }

                @Override
                public String getName() {
                    return "Configure role-mapper";
                }
            });

            elements.add(SimpleSocketBinding.builder().withName(DEFAULT).withPort(PORT_DEFAULT).build());
            elements.add(SimpleRemotingConnector.builder().withName(DEFAULT).withSocketBinding(DEFAULT)
                    .withSaslAuthenticationFactory(DEFAULT_SASL_AUTHENTICATION).build());

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }
    }
}
