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
package org.jboss.as.test.integration.security.aselytron;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.RolesPrintingServletUtils;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for usage PicketBox security domain as Elytron security realm in Elytron security domain.
 *
 * @author olukas
 */
@ServerSetup({SecurityDomainAsElytronSecurityRealmTestCase.PropertyFilesSetup.class,
    SecurityDomainAsElytronSecurityRealmTestCase.LegacySecurityDomainsSetup.class,
    SecurityDomainAsElytronSecurityRealmTestCase.ElytronRelatedSetup.class})
@RunAsClient
@RunWith(Arquillian.class)
public class SecurityDomainAsElytronSecurityRealmTestCase {

    private static final String PB_ONE_LOGIN_MODULE = "pb-one-login-module";
    private static final String PB_TWO_LOGIN_MODULES = "pb-two-login-modules";
    private static final String PB_WITH_MAPPING_MODULE = "pb-with-mapping-module";
    private static final String ELY_ONE_LOGIN_MODULE = "ely-one-login-module";
    private static final String ELY_TWO_LOGIN_MODULES = "ely-two-login-modules";
    private static final String ELY_WITH_MAPPING_MODULE = "ely-with-mapping-module";

    private static final String USER1 = "user1";
    private static final String USER2 = "user2";
    private static final String USER_WITHOUT_ROLE = "userWithoutRole";
    private static final String PASSWORD1 = "password1";
    private static final String PASSWORD2 = "password2";

    private static final String ROLE1 = "role1";
    private static final String ROLE2 = "role2";
    private static final String ROLE3 = "role3";
    private static final String ROLE_FROM_MAPPING_MODULE = "roleFromMappingModule";

    private final String[] allPossibleRoles = {ROLE1, ROLE2, ROLE3, ROLE_FROM_MAPPING_MODULE};

    @Deployment(name = PB_ONE_LOGIN_MODULE)
    public static WebArchive directAccessToLegacyDomainDeployment() {
        return createWar(PB_ONE_LOGIN_MODULE);
    }

    @Deployment(name = ELY_ONE_LOGIN_MODULE)
    public static WebArchive securityDomainWithOneLoginModuleDeployment() {
        return createWar(ELY_ONE_LOGIN_MODULE);
    }

    @Deployment(name = ELY_TWO_LOGIN_MODULES)
    public static WebArchive securityDomainWithTwoLoginModulesDeployment() {
        return createWar(ELY_TWO_LOGIN_MODULES);
    }

    @Deployment(name = ELY_WITH_MAPPING_MODULE)
    public static WebArchive securityDomainWithMappingModuleDeployment() {
        return createWar(ELY_WITH_MAPPING_MODULE);
    }

    private static WebArchive createWar(String securityDomainName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, securityDomainName + ".war");
        war.addClasses(RolePrintingServlet.class);
        war.addAsWebInfResource(SecurityDomainAsElytronSecurityRealmTestCase.class.getPackage(),
                SecurityDomainAsElytronSecurityRealmTestCase.class.getSimpleName() + "-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(securityDomainName), "jboss-web.xml");
        return war;
    }

    /**
     * Test whether access through PicketBox security domain is still possible, even if this domain is exported as Elytron
     * security realm.
     */
    @Test
    @OperateOnDeployment(PB_ONE_LOGIN_MODULE)
    public void testDirectAccessToLegacyDomain(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        String responseBody = Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER1, PASSWORD1, SC_OK);

        String[] expectedRoles = {ROLE1};
        RolesPrintingServletUtils.assertExpectedRoles(responseBody, allPossibleRoles, expectedRoles);
    }

    /**
     * Test whether user with correct credentials has granted access to application secured by Elytron security domain which
     * uses PicketBox security domain as Elytron security realm. It also checks whether just a correct roles are assigned to the
     * user.
     */
    @Test
    @OperateOnDeployment(ELY_ONE_LOGIN_MODULE)
    public void testAccessAndRoleAssignement(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        String responseBody = Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER1, PASSWORD1, SC_OK);

        String[] expectedRoles = {ROLE1};
        RolesPrintingServletUtils.assertExpectedRoles(responseBody, allPossibleRoles, expectedRoles);
    }

    /**
     * Test whether forbidden (HTTP status 403) is returned by application secured by Elytron security domain which uses
     * PicketBox security domain as Elytron security realm when it is accessed by user with correct credentials but without any
     * role.
     */
    @Test
    @OperateOnDeployment(ELY_ONE_LOGIN_MODULE)
    public void testAccessWithoutRole(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER_WITHOUT_ROLE, PASSWORD1, SC_FORBIDDEN);
    }

    /**
     * Test whether user with wrong password has denied access to application secured by Elytron security domain which uses
     * PicketBox security domain as Elytron security realm.
     */
    @Test
    @OperateOnDeployment(ELY_ONE_LOGIN_MODULE)
    public void testWrongPassword(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER1, "wrongPassword", SC_UNAUTHORIZED);
    }

    /**
     * Test whether user with empty password has denied access to application secured by Elytron security domain which uses
     * PicketBox security domain as Elytron security realm.
     */
    @Test
    @OperateOnDeployment(ELY_ONE_LOGIN_MODULE)
    public void testEmptyPassword(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER1, "", SC_UNAUTHORIZED);
    }

    /**
     * Test whether non existing user has denied access to application secured by Elytron security domain which uses PicketBox
     * security domain as Elytron security realm.
     */
    @Test
    @OperateOnDeployment(ELY_ONE_LOGIN_MODULE)
    public void testNonExistingUser(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, "nonExistingUser", PASSWORD1, SC_UNAUTHORIZED);
    }

    /**
     * Given PicketBox security domain uses two login modules. First of them is sufficient and second is required. This test
     * checks whether user with correct credentials has granted access to application secured by Elytron security domain which
     * uses given PicketBox security domain as Elytron security realm in case when this user is included in first login module.
     * It also checks whether just a correct roles are assigned to the user.
     */
    @Test
    @OperateOnDeployment(ELY_TWO_LOGIN_MODULES)
    public void testAccessForTwoLoginModules_userInFirstmodule(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        String responseBody = Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER2, PASSWORD2, SC_OK);

        String[] expectedRoles = {ROLE2, ROLE3};
        RolesPrintingServletUtils.assertExpectedRoles(responseBody, allPossibleRoles, expectedRoles);
    }

    /**
     * Given PicketBox security domain uses two login modules. First of them is sufficient and second is required. This test
     * checks whether user with correct credentials has granted access to application secured by Elytron security domain which
     * uses given PicketBox security domain as Elytron security realm in case when this user is included in second login module.
     * It also checks whether just a correct roles are assigned to the user.
     */
    @Test
    @OperateOnDeployment(ELY_TWO_LOGIN_MODULES)
    public void testAccessForTwoLoginModules_userInSecondmodule(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        String responseBody = Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER1, PASSWORD1, SC_OK);

        String[] expectedRoles = {ROLE1};
        RolesPrintingServletUtils.assertExpectedRoles(responseBody, allPossibleRoles, expectedRoles);
    }

    /**
     * Given PicketBox security domain uses two login modules. First of them is sufficient and second is required. This test
     * checks whether user which does not exist in first or second login module has denied access to application secured by
     * Elytron security domain which uses PicketBox security domain as Elytron security realm.
     */
    @Test
    @OperateOnDeployment(ELY_TWO_LOGIN_MODULES)
    public void testAccessForTwoLoginModules_nonexistingUser(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, "nonExistingUser", PASSWORD1, SC_UNAUTHORIZED);
    }

    /**
     * Given PicketBox security domain includes also mapping module which maps some role to the user. This test checks whether
     * user has assigned also role from that mapping module.
     */
    @Test
    @OperateOnDeployment(ELY_WITH_MAPPING_MODULE)
    public void testAssignRoleFromMappingModule(@ArquillianResource URL webAppURL) throws Exception {
        URL prepareRolePrintingUrl = RolesPrintingServletUtils.prepareRolePrintingUrl(webAppURL, allPossibleRoles);
        String responseBody = Utils.makeCallWithBasicAuthn(prepareRolePrintingUrl, USER1, PASSWORD1, SC_OK);

        String[] expectedRoles = {ROLE1, ROLE_FROM_MAPPING_MODULE};
        RolesPrintingServletUtils.assertExpectedRoles(responseBody, allPossibleRoles, expectedRoles);
    }

    static class PropertyFilesSetup implements ServerSetupTask {

        private static final String USERS1_CONTENT = USER1 + "=" + PASSWORD1 + "\n"
                + USER_WITHOUT_ROLE + "=" + PASSWORD1;
        private static final String ROLES1_CONTENT = USER1 + "=" + ROLE1;
        private static final String USERS2_CONTENT = USER2 + "=" + PASSWORD2;
        private static final String ROLES2_CONTENT = USER2 + "=" + ROLE2 + "," + ROLE3;

        public static final File FILE_USERS1 = new File("users1.properties");
        public static final File FILE_ROLES1 = new File("roles1.properties");
        public static final File FILE_USERS2 = new File("users2.properties");
        public static final File FILE_ROLES2 = new File("roles2.properties");

        /**
         * Generates property files.
         */
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            FileUtils.writeStringToFile(FILE_USERS1, USERS1_CONTENT, "UTF-8");
            FileUtils.writeStringToFile(FILE_ROLES1, ROLES1_CONTENT, "UTF-8");
            FileUtils.writeStringToFile(FILE_USERS2, USERS2_CONTENT, "UTF-8");
            FileUtils.writeStringToFile(FILE_ROLES2, ROLES2_CONTENT, "UTF-8");
        }

        /**
         * Removes generated property files.
         */
        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            FILE_USERS1.delete();
            FILE_ROLES1.delete();
            FILE_USERS2.delete();
            FILE_ROLES2.delete();
        }
    }

    static class LegacySecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            final Map<String, String> lmOptions1 = new HashMap<>();
            lmOptions1.put("usersProperties", PropertyFilesSetup.FILE_USERS1.getAbsolutePath());
            lmOptions1.put("rolesProperties", PropertyFilesSetup.FILE_ROLES1.getAbsolutePath());
            final SecurityModule.Builder loginModuleBuilder1 = new SecurityModule.Builder()
                    .name("UsersRoles")
                    .flag("required")
                    .options(lmOptions1);
            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(PB_ONE_LOGIN_MODULE)
                    .loginModules(loginModuleBuilder1.build())
                    .build();

            final Map<String, String> lmOptions2 = new HashMap<>();
            lmOptions2.put("usersProperties", PropertyFilesSetup.FILE_USERS2.getAbsolutePath());
            lmOptions2.put("rolesProperties", PropertyFilesSetup.FILE_ROLES2.getAbsolutePath());
            final SecurityModule.Builder loginModuleBuilder2 = new SecurityModule.Builder()
                    .name("org.jboss.security.auth.spi.UsersRolesLoginModule")
                    .flag("sufficient")
                    .options(lmOptions2);
            final SecurityDomain sd2 = new SecurityDomain.Builder()
                    .name(PB_TWO_LOGIN_MODULES)
                    .loginModules(loginModuleBuilder2.build(), loginModuleBuilder1.build())
                    .build();

            final Map<String, String> mmOptions = new HashMap<>();
            mmOptions.put(USER1, ROLE_FROM_MAPPING_MODULE);
            final SecurityModule.Builder mappingModuleBuilder = new SecurityModule.Builder()
                    .name("SimpleRoles")
                    .flag("role")
                    .options(mmOptions);
            final SecurityDomain sd3 = new SecurityDomain.Builder()
                    .name(PB_WITH_MAPPING_MODULE)
                    .loginModules(loginModuleBuilder1.build())
                    .mappingModules(mappingModuleBuilder.build())
                    .build();

            return new SecurityDomain[]{sd1, sd2, sd3};
        }
    }

    static class ElytronRelatedSetup implements ServerSetupTask {

        private static final String SIMPLE_ROLE_DECODER = "test-simple-role-decoder";
        private static final String PREDEFINED_PERMISSION_MAPPER_NAME = "default-permission-mapper";
        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";

        @Override
        public void setup(ManagementClient managementClient, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format(
                        "/subsystem=elytron/simple-role-decoder=%s:add(attribute=Roles)", SIMPLE_ROLE_DECODER));
                prepareAuthenticationWithElytron(cli, ELY_ONE_LOGIN_MODULE, PB_ONE_LOGIN_MODULE);
                prepareAuthenticationWithElytron(cli, ELY_TWO_LOGIN_MODULES, PB_TWO_LOGIN_MODULES);
                prepareAuthenticationWithElytron(cli, ELY_WITH_MAPPING_MODULE, PB_WITH_MAPPING_MODULE);
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                removeAuthenticationWithElytron(cli, ELY_WITH_MAPPING_MODULE, PB_WITH_MAPPING_MODULE);
                removeAuthenticationWithElytron(cli, ELY_TWO_LOGIN_MODULES, PB_TWO_LOGIN_MODULES);
                removeAuthenticationWithElytron(cli, ELY_ONE_LOGIN_MODULE, PB_ONE_LOGIN_MODULE);
                cli.sendLine(String.format(
                        "/subsystem=elytron/simple-role-decoder=%s:remove()", SIMPLE_ROLE_DECODER));
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        private void prepareAuthenticationWithElytron(CLIWrapper cli, String elytronName, String legacySecurityDomain) {
            String exportedLegacySecurityDomain = legacySecurityDomain + "-exported";
            cli.sendLine(String.format(
                    "/subsystem=security/elytron-realm=%s:add(legacy-jaas-config=%s)", exportedLegacySecurityDomain,
                    legacySecurityDomain));
            cli.sendLine(String.format(
                    "/subsystem=elytron/security-domain=%s:add(default-realm=%s,permission-mapper=%s,realms=[{realm=%s,role-decoder=%s}])",
                    elytronName, exportedLegacySecurityDomain, PREDEFINED_PERMISSION_MAPPER_NAME, exportedLegacySecurityDomain,
                    SIMPLE_ROLE_DECODER));
            cli.sendLine(String.format(
                    "/subsystem=elytron/http-authentication-factory=%s:add(http-server-mechanism-factory=%s,security-domain=%s,"
                    + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"%s\"}]}])",
                    elytronName, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY, elytronName, elytronName));
            cli.sendLine(String.format(
                    "/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)", elytronName,
                    elytronName));
        }

        private void removeAuthenticationWithElytron(CLIWrapper cli, String elytronName, String legacySecurityDomain) {
            cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", elytronName));
            cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()", elytronName));
            cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()", elytronName));
            cli.sendLine(String.format("/subsystem=security/elytron-realm=%s:remove()", legacySecurityDomain + "-exported"));
        }
    }

}
