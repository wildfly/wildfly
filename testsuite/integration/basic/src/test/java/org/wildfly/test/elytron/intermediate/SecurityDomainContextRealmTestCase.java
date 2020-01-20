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
package org.wildfly.test.elytron.intermediate;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.ModelNodeUtil;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Test to check the login using the elytron intermediate configuration (elytron
 * relaying on an old security domain). This class just configures a
 * UsersRoles security domain and tests the login.
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({
    SecurityDomainContextRealmTestCase.SecurityDomainsSetup.class,
    SecurityDomainContextRealmTestCase.SecurityElytronRealmSetup.class,
    SecurityDomainContextRealmTestCase.ElytronSetup.class})
public class SecurityDomainContextRealmTestCase {

    private static final String DEPLOYMENT = "SecurityDomainContextRealmDep";
    private static final String SECURITY_DOMAIN_NAME = "SecurityDomainContextRealmSecDom";
    private static final String ELYTRON_REALM_NAME = "SecurityDomainContextRealmRealm";
    private static final String ELYTRON_DOMAIN_NAME = "SecurityDomainContextRealmDom";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deployment() {
        final Package currentPackage = SecurityDomainContextRealmTestCase.class.getPackage();
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war")
                .addClasses(PrincipalCounterServlet.class, LoginCounterLoginModule.class)
                .addAsWebInfResource(currentPackage, "security-domain-context-realm-web.xml", "web.xml")
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(ELYTRON_DOMAIN_NAME), "jboss-web.xml")
                .addAsResource(currentPackage, "users.properties", "users.properties")
                .addAsResource(currentPackage, "roles.properties", "roles.properties");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testLogin(@ArquillianResource URL webAppURL) throws Exception {
        URL url = new URL(webAppURL.toExternalForm() + PrincipalCounterServlet.SERVLET_PATH);
        // test login KO (401)
        Utils.makeCallWithBasicAuthn(url, "user1", "Bad-Password", HttpServletResponse.SC_UNAUTHORIZED);
        // test login OK but without Users role (403)
        Utils.makeCallWithBasicAuthn(url, "admin", "admin", HttpServletResponse.SC_FORBIDDEN);
        // test login OK
        String output = Utils.makeCallWithBasicAuthn(url, "user1", "password1", HttpServletResponse.SC_OK);
        Assert.assertThat("Username is correct", output, CoreMatchers.startsWith("user1:"));
        int counter = Integer.parseInt(output.substring("user1:".length()));
        // test login OK with cache
        output = Utils.makeCallWithBasicAuthn(url, "user1", "password1", HttpServletResponse.SC_OK);
        Assert.assertThat("Username is correct", output, CoreMatchers.startsWith("user1:"));
        // ensure cache is in place and the login counter has not been incremented
        int newCounter = Integer.parseInt(output.substring("user1:".length()));
        Assert.assertThat("Cache is working and same login count", newCounter, CoreMatchers.is(counter));
    }

    /**
     * Creates a security-domain with default cache to be used as legacy realm.
     * The LoginCounterLoginModule is added to count the number of logins.
     *
     * <security-domain name="SecurityDomainContextRealmSecDom" cache-type="default">
     *     <authentication>
     *         <login-module code="UsersRoles" flag="required">
     *             <module-option name="rolesProperties" value="/path/to/roles.properties"/>
     *             <module-option name="usersProperties" value="/path/to/users.properties"/>
     *             <module-option name="password-stacking" value="useFirstPass"/>
     *         </login-module>
     *         <login-module code="org.wildfly.test.elytron.LoginCounterLoginModule" flag="optional">
     *             <module-option name="password-stacking" value="useFirstPass"/>
     *         </login-module>
     *     </authentication>
     * </security-domain>
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            Map<String, String> lmOptions = new HashMap<>();
            String usersProperties = new File(SecurityDomainContextRealmTestCase.class.getResource("users.properties").getFile()).getAbsolutePath();
            String rolesProperties = new File(SecurityDomainContextRealmTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath();
            lmOptions.put("password-stacking", "useFirstPass");
            lmOptions.put("usersProperties", usersProperties);
            lmOptions.put("rolesProperties", rolesProperties);
            final SecurityModule.Builder userRolesBuilder = new SecurityModule.Builder()
                    .name("UsersRoles")
                    .options(lmOptions)
                    .flag("required");

            lmOptions = new HashMap<>();
            lmOptions.put("password-stacking", "useFirstPass");
            final SecurityModule.Builder counterBuilder = new SecurityModule.Builder()
                    .name(LoginCounterLoginModule.class.getName())
                    .options(lmOptions)
                    .flag("optional");

            final SecurityDomain sd = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME)
                    .cacheType("default")
                    .loginModules(userRolesBuilder.build(), counterBuilder.build())
                    .build();

            return new SecurityDomain[]{sd};
        }
    }

    /**
     * Creates the elytron realm mapper to use in the mixed configuration:
     *
     *  <security-realms>
     *      <elytron-realm name="SecurityDomainContextRealmRealm" legacy-jaas-config="SecurityDomainContextRealmSecDom"/>
     *  </security-realms>
     */
    static class SecurityElytronRealmSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient mc, String string) throws Exception {
            // /subsystem=security/elytron-realm=ELYTRON_REALM_NAME:add(legacy-jaas-config=SECURITY_DOMAIN_NAME)
            ModelNode op = Util.createAddOperation(
                PathAddress.pathAddress().append("subsystem", "security").append("elytron-realm", ELYTRON_REALM_NAME));
            ModelNodeUtil.setIfNotNull(op, "legacy-jaas-config", SECURITY_DOMAIN_NAME);
            CoreUtils.applyUpdate(op, mc.getControllerClient());
            ServerReload.reloadIfRequired(mc);
        }

        @Override
        public void tearDown(ManagementClient mc, String string) throws Exception {
            // /subsystem=security/elytron-realm=ELYTRON_REALM_NAME:remove
            CoreUtils.applyUpdate(Util.createRemoveOperation(
                    PathAddress.pathAddress().append("subsystem", "security").append("elytron-realm", ELYTRON_REALM_NAME)),
                    mc.getControllerClient());
            ServerReload.reloadIfRequired(mc);
        }
    }

    /**
     * Creates the elytron setup to use the legacy security domain.
     */
    static class ElytronSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[]{
                SimpleSecurityDomain.builder()
                    .withName(ELYTRON_DOMAIN_NAME)
                    .withDefaultRealm(ELYTRON_REALM_NAME)
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                        .withRealm(ELYTRON_REALM_NAME)
                        .build())
                    .build(),
                UndertowApplicationSecurityDomain.builder()
                    .withName(ELYTRON_DOMAIN_NAME)
                    .withSecurityDomain(ELYTRON_DOMAIN_NAME)
                    .build()
            };
        }

    }
}
