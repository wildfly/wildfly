/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.integration.elytron.realmmappers;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.CORRECT_PASSWORD;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.DEFAULT_REALM;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.REALM1;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.REALM2;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.SECURITY_DOMAIN_REFERENCE;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.USER_IN_DEFAULT_REALM;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.USER_IN_REALM1;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.util.JarUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.servlets.SecuredPrincipalPrintingServlet;

/**
 * Test case for 'custom-realm-mapper' Elytron subsystem resource.
 *
 * @author olukas
 * @author Hynek Švábek <hsvabek@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({RealmMapperServerSetupTask.class, CustomRealmMapperTestCase.SetupTask.class})
public class CustomRealmMapperTestCase {

    private static final String CUSTOM_REALM_MAPPER_MODULE_NAME = "org.jboss.customrealmmapperimpl";

    private static final String DEFAULT_REALM_MAPPER = "defaultRealmMapper";
    private static final String REALM1_MAPPER = "realm1Mapper";
    private static final String REALM2_MAPPER = "realm2Mapper";
    private static final String NON_EXIST_MAPPER = "nonExistMapper";
    protected static final String DEPLOYMENT = "dep";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive createWar() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war").addClasses(SecuredPrincipalPrintingServlet.class)
            .addAsWebInfResource(Utils.getJBossWebXmlAsset(SECURITY_DOMAIN_REFERENCE), "jboss-web.xml")
            .addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml");
    }

    /**
     * Test whether default realm is used in security domain when no realm-mapper is configured.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testDefaultRealmWithoutAnyRealmMapper(@ArquillianResource URL webAppURL) throws Exception {
        assertEquals("Response body is not correct.", USER_IN_DEFAULT_REALM,
                Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_DEFAULT_REALM, CORRECT_PASSWORD, SC_OK));
        Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_REALM1, CORRECT_PASSWORD, SC_UNAUTHORIZED);
    }

    /**
     * Test whether custom realm mapper return expected value. It means that security domain uses expected realm instead of
     * default.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testRealmMapper(@ArquillianResource URL webAppURL) throws Exception {
        setupRealmMapper(REALM1_MAPPER);
        try {
            assertEquals("Response body is not correct.", USER_IN_REALM1,
                    Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_REALM1, CORRECT_PASSWORD, SC_OK));
            Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_DEFAULT_REALM, CORRECT_PASSWORD, SC_UNAUTHORIZED);
        } finally {
            undefineRealmMapper();
        }
    }

    static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                File moduleJar = JarUtils.createJarFile("testJar", CustomRealmMapperImpl.class);
                cli.sendLine("module add --name=" + CUSTOM_REALM_MAPPER_MODULE_NAME
                    + " --slot=main --dependencies=org.wildfly.security.elytron,org.wildfly.extension.elytron --resources="
                    + moduleJar.getAbsolutePath());

                cli.sendLine(String.format(
                    "/subsystem=elytron/custom-realm-mapper=%s:add(class-name=%s, module=%s, configuration={mapTo=%s})",
                    DEFAULT_REALM_MAPPER, CustomRealmMapperImpl.class.getName(), CUSTOM_REALM_MAPPER_MODULE_NAME,
                    DEFAULT_REALM));
                cli.sendLine(String.format(
                    "/subsystem=elytron/custom-realm-mapper=%s:add(class-name=%s, module=%s, configuration={mapTo=%s})",
                    REALM1_MAPPER, CustomRealmMapperImpl.class.getName(), CUSTOM_REALM_MAPPER_MODULE_NAME, REALM1));
                cli.sendLine(String.format(
                    "/subsystem=elytron/custom-realm-mapper=%s:add(class-name=%s, module=%s, configuration={mapTo=%s})",
                    REALM2_MAPPER, CustomRealmMapperImpl.class.getName(), CUSTOM_REALM_MAPPER_MODULE_NAME, REALM2));
                cli.sendLine(String.format(
                    "/subsystem=elytron/custom-realm-mapper=%s:add(class-name=%s, module=%s, configuration={mapTo=%s})",
                    NON_EXIST_MAPPER, CustomRealmMapperImpl.class.getName(), CUSTOM_REALM_MAPPER_MODULE_NAME, "nonExistRealm"));

            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/custom-realm-mapper=%s:remove()",
                        DEFAULT_REALM_MAPPER));
                cli.sendLine(String.format("/subsystem=elytron/custom-realm-mapper=%s:remove()",
                        REALM1_MAPPER));
                cli.sendLine(String.format("/subsystem=elytron/custom-realm-mapper=%s:remove()",
                        REALM2_MAPPER));
                cli.sendLine(String.format("/subsystem=elytron/custom-realm-mapper=%s:remove()",
                        NON_EXIST_MAPPER));
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

    }

    protected void setupRealmMapper(String realmMapperName) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:write-attribute(name=realm-mapper,value=%s)",
                RealmMapperServerSetupTask.SECURITY_DOMAIN_NAME, realmMapperName));
            cli.sendLine(String.format("reload"));
        }
    }

    protected void undefineRealmMapper() throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:undefine-attribute(name=realm-mapper)",
                RealmMapperServerSetupTask.SECURITY_DOMAIN_NAME));
            cli.sendLine(String.format("reload"));
        }
    }

    protected URL principalServlet(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm() + SecuredPrincipalPrintingServlet.SERVLET_PATH.substring(1));
    }
}
