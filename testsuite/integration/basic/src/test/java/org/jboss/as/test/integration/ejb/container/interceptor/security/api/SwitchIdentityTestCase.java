/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.container.interceptor.security.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ejb.EJBAccessException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.container.interceptor.security.CurrentUserCredential;
import org.jboss.as.test.integration.ejb.container.interceptor.security.GuestDelegationLoginModule;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.jboss.security.ClientLoginModule;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Property;
import org.xnio.Sequence;

/**
 * Testcase based on ejb-security-interceptors quickstart application. It tests security context propagation for EJBs.
 *
 * @author Josef Cacek
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({ SwitchIdentityTestCase.SecurityDomainsSetup.class, //
        SwitchIdentityTestCase.SecurityRealmsSetup.class})
@RunAsClient
public class SwitchIdentityTestCase {

    private static final String EJB_OUTBOUND_REALM = "ejb-outbound-realm";
    private static final String SECURITY_DOMAIN_NAME = "switch-identity-test";

    private final Map<String, String> passwordsToUse;

    public SwitchIdentityTestCase() {
        passwordsToUse = new HashMap<>();
        passwordsToUse.put("guest", "guest");
        passwordsToUse.put("user1", "password1");
        passwordsToUse.put("user2", "password2");
        passwordsToUse.put("remoteejbuser", "rem@teejbpasswd1");
    }

    @ArquillianResource
    private ManagementClient mgmtClient;

    /**
     * The login {@link Configuration} which always returns a single {@link AppConfigurationEntry} with a
     * {@link ClientLoginModule}.
     */
    private static final Configuration CLIENT_LOGIN_CONFIG = new Configuration() {

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            final Map<String, String> options = new HashMap<String, String>();
            options.put("multi-threaded", "true");
            options.put("restore-login-identity", "true");

            AppConfigurationEntry clmEntry = new AppConfigurationEntry(ClientLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);

            return new AppConfigurationEntry[] { clmEntry };
        }
    };

    // Public methods --------------------------------------------------------

    @BeforeClass
    public static void beforeClass() {
        AssumeTestGroupUtil.assumeElytronProfileEnabled(); // PicketBox specific feature - not supported in Elytron
    }

    /**
     * Creates a deployment application for this test.
     *
     * @return
     * @throws IOException
     */
    @Deployment
    public static JavaArchive createDeployment() throws IOException {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, EJBUtil.APPLICATION_NAME + ".jar");
        jar.addClasses(GuestDelegationLoginModule.class, EJBUtil.class, Manage.class, BridgeBean.class, TargetBean.class,
                CurrentUserCredential.class, ServerSecurityInterceptor.class, ClientSecurityInterceptor.class);
        jar.addAsManifestResource(SwitchIdentityTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(ClientSecurityInterceptor.class.getPackage(), "permissions.xml", "permissions.xml");
        jar.addAsManifestResource(Utils.getJBossDeploymentStructure("org.jboss.as.security-api", "org.jboss.as.core-security-api"), "jboss-deployment-structure.xml");
        return jar;
    }

    /**
     * Test identity propagation using SecurityContextAssociation API from the client.
     *
     * @throws Exception
     */
    @Test
    public void testSecurityContextAssociation() throws Exception {
        callUsingSecurityContextAssociation("guest", false, false);
        callUsingSecurityContextAssociation("user1", true, false);
        callUsingSecurityContextAssociation("user2", false, true);
    }

    /**
     * Test identity propagation using LoginContext API from the client.
     *
     * @throws Exception
     */
    @Test
    public void testClientLoginModule() throws Exception {
        callUsingClientLoginModule("guest", false, false);
        callUsingClientLoginModule("user1", true, false);
        callUsingClientLoginModule("user2", false, true);
    }

    // Private methods -------------------------------------------------------

    /**
     * Perform the tests using the ClientLoginModule and LoginContext API to set the desired Principal.
     */
    private void callUsingClientLoginModule(String userName, boolean hasRole1, boolean hasRole2) throws Exception {
        AuthenticationContext authenticationContext = setupAuthenticationContext(userName);
        authenticationContext.runCallable(() -> {

            // register the client side interceptor
            final EJBClientContext ejbClientContext = EJBClientContext.getCurrent().withAddedInterceptors(new ClientSecurityInterceptor());
            ejbClientContext.runCallable(() -> {
                final Manage targetBean = EJBUtil.lookupEJB(TargetBean.class, Manage.class);
                final Manage bridgeBean = EJBUtil.lookupEJB(BridgeBean.class, Manage.class);

                //test direct access
                testMethodAccess(targetBean, ManageMethodEnum.ALLROLES, true);
                testMethodAccess(targetBean, ManageMethodEnum.ROLE1, hasRole1);
                testMethodAccess(targetBean, ManageMethodEnum.ROLE2, hasRole2);

                //test security context propagation
                testMethodAccess(bridgeBean, ManageMethodEnum.ALLROLES, true);
                testMethodAccess(bridgeBean, ManageMethodEnum.ROLE1, hasRole1);
                testMethodAccess(bridgeBean, ManageMethodEnum.ROLE2, hasRole2);
                return null;
            });
            return null;
        });
    }

    private AuthenticationContext setupAuthenticationContext(final String username) {
        OptionMap.Builder builder = OptionMap.builder().set(Options.SASL_POLICY_NOANONYMOUS, true);
        builder.set(Options.SASL_POLICY_NOPLAINTEXT, false);
        builder.set(Options.SASL_DISALLOWED_MECHANISMS, Sequence.of("JBOSS-LOCAL-USER"));

        final AuthenticationContext authenticationContext = AuthenticationContext.empty()
                .with(
                        MatchRule.ALL,
                        AuthenticationConfiguration.EMPTY
                                .useName(username == null ? "$local" : username)
                                .useRealm(null)
                                .usePassword(passwordsToUse.getOrDefault(username, ""))
                                .setSaslMechanismSelector(SaslMechanismSelector.fromString("DIGEST-MD5"))
                                .useMechanismProperties(getSaslProperties(builder.getMap()))
                                .useProvidersFromClassLoader(org.jboss.as.test.integration.ejb.container.interceptor.security.SwitchIdentityTestCase.class.getClassLoader()));

        return authenticationContext;
    }

    private Map<String, String> getSaslProperties(final OptionMap connectionCreationOptions) {
        Map<String, String> saslProperties = null;
        Sequence<Property> value = connectionCreationOptions.get(Options.SASL_PROPERTIES);
        if (value != null) {
            saslProperties = new HashMap<>(value.size());
            for (Property property : value) {
                saslProperties.put(property.getKey(), (String) property.getValue());
            }
        }
        return saslProperties;
    }

    /**
     * Perform the tests using the SecurityContextAssociation API to set the desired Principal.
     */
    private void callUsingSecurityContextAssociation(String userName, boolean hasRole1, boolean hasRole2) throws Exception {
        try {
            final Properties ejbClientConfiguration = EJBUtil.createEjbClientConfiguration(Utils.getHost(mgmtClient), userName);

            // register the client side interceptor
            final EJBClientContext ejbClientContext = EJBClientContext.getCurrent().withAddedInterceptors(new org.jboss.as.test.integration.ejb.container.interceptor.security.ClientSecurityInterceptor());
            SecurityContextAssociation.setPrincipal(new SimplePrincipal(userName));

            ejbClientContext.runCallable(() -> {
                final Manage targetBean = EJBUtil.lookupEJB(ejbClientConfiguration, TargetBean.class, Manage.class);
                final Manage bridgeBean = EJBUtil.lookupEJB(ejbClientConfiguration, BridgeBean.class, Manage.class);

                //test direct access
                testMethodAccess(targetBean, ManageMethodEnum.ALLROLES, true);
                testMethodAccess(targetBean, ManageMethodEnum.ROLE1, hasRole1);
                testMethodAccess(targetBean, ManageMethodEnum.ROLE2, hasRole2);

                //test security context propagation
                testMethodAccess(bridgeBean, ManageMethodEnum.ALLROLES, true);
                testMethodAccess(bridgeBean, ManageMethodEnum.ROLE1, hasRole1);
                testMethodAccess(bridgeBean, ManageMethodEnum.ROLE2, hasRole2);
                return null;
            });
        } finally {
            SecurityContextAssociation.clearSecurityContext();
        }
    }

    /**
     * Tests access to a single method of a {@link Manage} EJB implementation.
     *
     * @param bean EJB instance
     * @param method method type
     * @param hasAccess expected value
     */
    private void testMethodAccess(Manage bean, ManageMethodEnum method, boolean hasAccess) {
        try {
            final String result;
            switch (method) {
                case ROLE1:
                    result = bean.role1();
                    break;
                case ROLE2:
                    result = bean.role2();
                    break;
                case ALLROLES:
                    result = bean.allRoles();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown bean method type.");
            }
            assertEquals(Manage.RESULT, result);
            if (!hasAccess) {
                fail("Acess should be denied.");
            }
        } catch (EJBAccessException e) {
            if (hasAccess) {
                fail("Access should be allowed.");
            }
        }

    }

    // Embedded classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         *
         * <pre>
         * &lt;security-domain name=&quot;switch-identity-test&quot; cache-type=&quot;default&quot;&gt;
         *     &lt;authentication&gt;
         *         &lt;login-module code=&quot;{@link GuestDelegationLoginModule}&quot; flag=&quot;optional&quot;&gt;
         *             &lt;module-option name=&quot;password-stacking&quot; value=&quot;useFirstPass&quot;/&gt;
         *         &lt;/login-module&gt;
         *         &lt;login-module code=&quot;Remoting&quot; flag=&quot;optional&quot;&gt;
         *             &lt;module-option name=&quot;password-stacking&quot; value=&quot;useFirstPass&quot;/&gt;
         *         &lt;/login-module&gt;
         *         &lt;login-module code=&quot;RealmDirect&quot; flag=&quot;required&quot;&gt;
         *             &lt;module-option name=&quot;password-stacking&quot; value=&quot;useFirstPass&quot;/&gt;
         *         &lt;/login-module&gt;
         *     &lt;/authentication&gt;
         * &lt;/security-domain&gt;
         * </pre>
         *
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            final SecurityModule.Builder loginModuleBuilder = new SecurityModule.Builder().flag("optional").putOption(
                    "password-stacking", "useFirstPass");
            final SecurityDomain sd = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME)
                    .loginModules(loginModuleBuilder.name(GuestDelegationLoginModule.class.getName()).build(),
                            loginModuleBuilder.name("Remoting").build(), //
                            loginModuleBuilder.name("RealmDirect").build()) //
                    .build();
            return new SecurityDomain[] { sd };
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates security realms for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityRealmsSetup extends AbstractSecurityRealmsServerSetupTask {

        /**
         * Returns SecurityRealms configuration for this testcase.
         *
         * <pre>
         * &lt;security-realm name=&quot;ejb-outbound-realm&quot;&gt;
         *   &lt;server-identities&gt;
         *      &lt;secret value=&quot;xxx&quot;/&gt;
         *   &lt;/server-identities&gt;
         * &lt;/security-realm&gt;
         * </pre>
         *
         */
        @Override
        protected SecurityRealm[] getSecurityRealms() {
            final ServerIdentity serverIdentity = new ServerIdentity.Builder().secretPlain(EJBUtil.CONNECTION_PASSWORD).build();
            final SecurityRealm realm = new SecurityRealm.Builder().name(EJB_OUTBOUND_REALM).serverIdentity(serverIdentity)
                    .build();
            return new SecurityRealm[] { realm };
        }
    }

    /**
     * An Enum, which holds expected method types in {@link Manage} interface.
     *
     * @author Josef Cacek
     */
    private enum ManageMethodEnum {
        ROLE1, ROLE2, ALLROLES
    }

}
