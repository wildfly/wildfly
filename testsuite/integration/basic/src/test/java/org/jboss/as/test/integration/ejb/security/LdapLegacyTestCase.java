/*
 * Copyright 2020 Red Hat, Inc.
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
package org.jboss.as.test.integration.ejb.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.ejb.security.authorization.SaslLegacyMechanismBean;
import org.jboss.as.test.integration.ejb.security.authorization.SaslLegacyMechanismBeanRemote;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.loginmodules.LdapExtLDAPServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.RequestSendFailedException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/**
 * EJB test to check a legacy ldap security realm configuration. The remoting
 * endpoint is configured to use an ldap realm and the EJB is also setup to
 * use a security domain that points to that realm (RealmDirect).
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({LdapExtLDAPServerSetupTask.class, LdapLegacyTestCase.LdapRealmSetup.class, LdapLegacyTestCase.SecurityDomainsSetup.class})
public class LdapLegacyTestCase {

    private static final String MODULE = "LdapLegacyTestCase";
    private static final String LDAP_CONNECTION = MODULE + "-con";
    private static final String LDAP_REALM = MODULE + "-ldap-realm";
    private static final String SECURITY_DOMAIN = MODULE + "-domain";

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() {
        // PicketBox specific feature - not supported in Elytron
        AssumeTestGroupUtil.assumeElytronProfileEnabled();
    }

    @Deployment(name = MODULE + ".jar", order = 1, testable = false)
    public static Archive<JavaArchive> testAppDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE + ".jar")
                .addClass(SaslLegacyMechanismBean.class)
                .addClass(SaslLegacyMechanismBeanRemote.class)
                .addAsManifestResource(LdapLegacyTestCase.class.getPackage(), MODULE + "-ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(LdapLegacyTestCase.class.getPackage(), MODULE + "-jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    /**
     * Creates a properties with the old configuration for connection.
     * If the username or password parameters are null, no authentication
     * properties are added.
     *
     * @param username The username to connect or null
     * @param password The password of the user or null
     * @return The properties to use in the InitialContext
     * @throws IOException Error reading the properties file
     */
    private Properties setupEJBClientProperties(String username, String password) throws IOException {
        final String clientPropertiesFile = "org/jboss/as/test/integration/ejb/security/jboss-ejb-client.properties";
        final InputStream inputStream = SaslLegacyMechanismConfigurationTestCase.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);

        properties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            properties.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        if (username != null && password != null) {
            properties.put(Context.SECURITY_PRINCIPAL, username);
            properties.put(Context.SECURITY_CREDENTIALS, password);
            properties.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "true");
        } else {
            properties.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
        }

        return properties;
    }

    /**
     * Test the EJB call with that username and password (can be null for
     * anonymous call).
     *
     * @param username The username to connect or null
     * @param password The password of the user or null
     * @return The principal name returned by the SaslLegacyMechanismBean
     * @throws Exception Some error calling the EJB
     */
    private String testEjb(String username, String password) throws Exception {
        InitialContext ctx = null;
        try {
            ctx = new InitialContext(setupEJBClientProperties(username, password));
            SaslLegacyMechanismBeanRemote bean = (SaslLegacyMechanismBeanRemote) ctx.lookup(
                    "ejb:/" + MODULE + "/" + SaslLegacyMechanismBean.class.getSimpleName() + "!" + SaslLegacyMechanismBeanRemote.class.getCanonicalName());
            return bean.getPrincipal();
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void testAnonymous() throws Exception {
        exceptionRule.expect(RequestSendFailedException.class);
        testEjb(null, null);
    }

    @Test
    public void testInvalidUser() throws Exception {
        exceptionRule.expect(RequestSendFailedException.class);
        testEjb("invalid-user", "invalid-password");
    }

    @Test
    public void testInvalidPassword() throws Exception {
        exceptionRule.expect(RequestSendFailedException.class);
        testEjb("jduke", "invalid-password");
    }

    @Test
    public void testNotAllowedUserWithoutRole() throws Exception {
        exceptionRule.expect(EJBAccessException.class);
        exceptionRule.expectMessage("WFLYEJB0364");
        testEjb("tester", "password");
    }

    @Test
    public void testValid() throws Exception {
        Assert.assertEquals("jduke", testEjb("jduke", "theduke"));
    }

    /**
     * Creates the security domain to use in the application that uses the ldap realm.
     * It's just a RealmDirect to use the same ldap realm created in the other task.
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            Map<String, String> lmOptions = new HashMap<>();
            lmOptions.put("password-stacking", "useFirstPass");
            final SecurityModule.Builder remotingBuilder = new SecurityModule.Builder()
                    .name("Remoting")
                    .options(lmOptions)
                    .flag("optional");

            lmOptions = new HashMap<>();
            lmOptions.put("password-stacking", "useFirstPass");
            lmOptions.put("realm", LDAP_REALM);
            final SecurityModule.Builder realmBuilder = new SecurityModule.Builder()
                    .name("RealmDirect")
                    .options(lmOptions)
                    .flag("required");

            final SecurityDomain sd = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN)
                    .cacheType("default")
                    .loginModules(remotingBuilder.build(), realmBuilder.build())
                    .build();

            return new SecurityDomain[]{sd};
        }
    }

    /**
     * Task that creates a new ldap realm and assigns it to the remoting interface.
     */
    static class LdapRealmSetup implements ServerSetupTask {

        private String previousSaslAuthenticationFactory = null;

        @Override
        public void setup(ManagementClient mc, String string) throws Exception {
            // save the previous realm used at remoting
            ModelNode op = Util.createOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION,
                    PathAddress.pathAddress().append("subsystem", "remoting").append("http-connector", "http-remoting-connector"));
            op.get("name").set("sasl-authentication-factory");
            ModelNode result = mc.getControllerClient().execute(op);
            Assert.assertEquals("The read-attribute to get the current security-realm in remoting is a success",
                    "success", result.get("outcome").asString());
            if (result.get("result").isDefined()) {
                previousSaslAuthenticationFactory = result.get("result").asString();

                // create the ldap realm and assign it to the remoting endpoint
                try (CLIWrapper cli = new CLIWrapper(true)) {
                    cli.sendLine("batch");
                    cli.sendLine(String.format("/core-service=management/ldap-connection=\"%s\":add(url=\"%s\", search-dn=\"%s\", search-credential=\"%s\")",
                            LDAP_CONNECTION, "ldap://" + Utils.getSecondaryTestAddress(mc) + ":" + LdapExtLDAPServerSetupTask.LDAP_PORT,
                            LdapExtLDAPServerSetupTask.SECURITY_PRINCIPAL, LdapExtLDAPServerSetupTask.SECURITY_CREDENTIALS));
                    cli.sendLine(String.format("/core-service=management/security-realm=\"%s\":add()", LDAP_REALM));
                    cli.sendLine(String.format("/core-service=management/security-realm=\"%s\"/authentication=ldap:add(connection=\"%s\", base-dn=\"ou=People,dc=jboss,dc=org\", username-attribute=uid)",
                            LDAP_REALM, LDAP_CONNECTION));
                    cli.sendLine(String.format("/core-service=management/security-realm=\"%s\"/authorization=ldap:add(connection=\"%s\")",
                            LDAP_REALM, LDAP_CONNECTION));
                    cli.sendLine(String.format("/core-service=management/security-realm=\"%s\"/authorization=ldap/group-search=group-to-principal:add( "
                            + "group-name=SIMPLE, group-name-attribute=cn, base-dn=\"ou=Roles,dc=jboss,dc=org\", principal-attribute=member, search-by=DISTINGUISHED_NAME)",
                            LDAP_REALM));
                    cli.sendLine("/subsystem=remoting/http-connector=http-remoting-connector:undefine-attribute(name=sasl-authentication-factory)");
                    cli.sendLine(String.format("/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=security-realm, value=\"%s\")",
                            LDAP_REALM));
                    cli.sendLine("run-batch");
                }

                ServerReload.reloadIfRequired(mc);
            }
        }

        @Override
        public void tearDown(ManagementClient mc, String string) throws Exception {
            // set back the original security realm at remoting and remove the new ldap realm
            if (previousSaslAuthenticationFactory != null) {
                try (CLIWrapper cli = new CLIWrapper(true)) {
                    cli.sendLine("batch");
                    cli.sendLine("/subsystem=remoting/http-connector=http-remoting-connector:undefine-attribute(name=security-realm)");
                    cli.sendLine(String.format("/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=sasl-authentication-factory, value=\"%s\")", previousSaslAuthenticationFactory));
                    cli.sendLine(String.format("/core-service=management/security-realm=\"%s\":remove", LDAP_REALM));
                    cli.sendLine(String.format("/core-service=management/ldap-connection=\"%s\":remove", LDAP_CONNECTION));
                    cli.sendLine("run-batch");
                }
            }

            ServerReload.reloadIfRequired(mc);
        }
    }
}
