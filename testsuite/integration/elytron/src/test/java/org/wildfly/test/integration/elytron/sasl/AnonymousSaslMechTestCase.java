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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.wildfly.test.integration.elytron.sasl.AbstractSaslTestBase.JmsSetup;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantPermissionMapper;
import org.wildfly.test.security.common.elytron.ConstantRoleMapper;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.PermissionRef;
import org.wildfly.test.security.common.elytron.SaslFilter;
import org.wildfly.test.security.common.elytron.SimpleConfigurableSaslServerFactory;
import org.wildfly.test.security.common.elytron.SimpleSaslAuthenticationFactory;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain.SecurityDomainRealm;
import org.wildfly.test.security.common.other.MessagingElytronDomainConfigurator;
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
@ServerSetup({ JmsSetup.class, AnonymousSaslMechTestCase.ServerSetup.class })
public class AnonymousSaslMechTestCase extends AbstractSaslTestBase {

    private static final String ANONYMOUS = "ANONYMOUS";
    private static final int PORT_ANONYMOUS = 10567;

    /**
     * Tests that client is able to use ANONYMOUS SASL mechanism when server allows it.
     */
    @Test
    public void testAnonymousAccess() throws Exception {
        AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty()
                        .setSaslMechanismSelector(SaslMechanismSelector.fromString(ANONYMOUS)))
                .run(() -> sendAndReceiveMsg(PORT_ANONYMOUS, false));
    }

    /**
     * Setup task which configures Elytron security domains and remoting connectors for this test.
     */
    public static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(ConstantPermissionMapper.builder().withName(NAME)
                    .withPermissions(PermissionRef.fromPermission(new LoginPermission())).build());
            elements.add(ConstantRoleMapper.builder().withName(NAME).withRoles("guest").build());
            elements.add(SimpleSecurityDomain.builder().withName(NAME).withDefaultRealm("ApplicationRealm").withRoleMapper(NAME)
                    .withPermissionMapper(NAME).withRealms(SecurityDomainRealm.builder().withRealm("ApplicationRealm").build())
                    .build());

            elements.add(SimpleConfigurableSaslServerFactory.builder().withName(ANONYMOUS).withSaslServerFactory("elytron")
                    .addFilter(SaslFilter.builder().withPatternFilter(ANONYMOUS).build()).build());
            elements.add(SimpleSaslAuthenticationFactory.builder().withName(ANONYMOUS).withSaslServerFactory(ANONYMOUS)
                    .withSecurityDomain(NAME)
                    .addMechanismConfiguration(MechanismConfiguration.builder().withMechanismName(ANONYMOUS).build()).build());

            elements.add(MessagingElytronDomainConfigurator.builder().withElytronDomain(NAME).build());

            elements.add(SimpleSocketBinding.builder().withName(ANONYMOUS).withPort(PORT_ANONYMOUS).build());
            elements.add(SimpleRemotingConnector.builder().withName(ANONYMOUS).withSocketBinding(ANONYMOUS)
                    .withSaslAuthenticationFactory(ANONYMOUS).build());

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }
    }
}
