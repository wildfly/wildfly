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

package org.wildfly.test.integration.elytron.permissionmappers;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AllPermission;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.util.JarUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.batch.jberet.deployment.BatchPermission;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.security.permission.NoPermission;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain.SecurityDomainRealm;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;
import org.wildfly.test.security.servlets.CheckIdentityPermissionServlet;

/**
 * Test for "custom-permission-mapper" Elytron resource. It tests if the defined permissions are correctly mapped to users.
 *
 * @author Josef Cacek
 * @author Hynek Švábek <hsvabek@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ CustomPermissionMapperTestCase.ServerSetup.class })
public class CustomPermissionMapperTestCase {

    private static final String CUSTOM_PERMISSION_MAPPER_MODULE_NAME = "org.jboss.custompermissionmapperimpl";

    private static final String SD_ALL = "custom-all-permission";
    private static final String SD_NO = "no-all-permission";

    private static final String TARGET_NAME_START = "start";


    @Deployment(testable = false, name = SD_ALL)
    public static WebArchive deployment2() {
        return createWar(SD_ALL);
    }

    @Deployment(testable = false, name = SD_NO)
    public static WebArchive deployment3() {
        return createWar(SD_NO);
    }

    /**
     * Tests security domain which contains custom-permission-mapper with AllPermission.
     */
    @Test
    @OperateOnDeployment(SD_ALL)
    public void testAllPermission(@ArquillianResource URL url) throws Exception {
        // anonymous
        assertUserHasPermission(url, null, null, AllPermission.class.getName(), null, null);
        assertUserHasPermission(url, null, null, LoginPermission.class.getName(), null, null);
        // valid user
        assertUserHasPermission(url, "guest", "guest", LoginPermission.class.getName(), null, null);
        assertUserHasPermission(url, "guest", "guest", BatchPermission.class.getName(), TARGET_NAME_START, null);
        assertUserHasPermission(url, "guest", "guest", NoPermission.class.getName(), null, null);
    }

    /**
     * Tests security domain which contains custom-permission-mapper without permissions.
     */
    @Test
    @OperateOnDeployment(SD_NO)
    public void testNoPermission(@ArquillianResource URL url) throws Exception {
        // anonymous
        assertUserHasntPermission(url, null, null, AllPermission.class.getName(), null, null);
        assertUserHasntPermission(url, null, null, LoginPermission.class.getName(), null, null);
        // valid user
        assertUserHasntPermission(url, "guest", "guest", LoginPermission.class.getName(), null, null);
        assertUserHasntPermission(url, "guest", "guest", BatchPermission.class.getName(), TARGET_NAME_START, null);
    }

    private void assertUserHasPermission(URL webappUrl, String user, String password, String className, String target,
            String action) throws Exception {
        assertEquals("true", doPermissionCheckPostReq(webappUrl, user, password, className, target, action));
    }

    private void assertUserHasntPermission(URL webappUrl, String user, String password, String className, String target,
            String action) throws Exception {
        assertEquals("false", doPermissionCheckPostReq(webappUrl, user, password, className, target, action));
    }

    /**
     * Makes request to {@link CheckIdentityPermissionServlet}.
     */
    private String doPermissionCheckPostReq(URL url, String user, String password, String className, String target,
            String action) throws URISyntaxException, UnsupportedEncodingException, IOException, ClientProtocolException {
        String body;
        final URI uri = new URI(url.toExternalForm() + CheckIdentityPermissionServlet.SERVLET_PATH.substring(1));
        final HttpPost post = new HttpPost(uri);
        List<NameValuePair> nvps = new ArrayList<>();
        setParam(nvps, CheckIdentityPermissionServlet.PARAM_USER, user);
        setParam(nvps, CheckIdentityPermissionServlet.PARAM_PASSWORD, password);
        setParam(nvps, CheckIdentityPermissionServlet.PARAM_CLASS, className);
        setParam(nvps, CheckIdentityPermissionServlet.PARAM_TARGET, target);
        setParam(nvps, CheckIdentityPermissionServlet.PARAM_ACTION, action);
        post.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (final CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == SC_FORBIDDEN && user != null) {
                    return Boolean.toString(false);
                }
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                body = EntityUtils.toString(response.getEntity());
            }
        }
        return body;
    }

    private void setParam(List<NameValuePair> nvps, final String paramName, String paramValue) {
        if (paramValue != null) {
            nvps.add(new BasicNameValuePair(paramName, paramValue));
        }
    }

    /**
     * Creates web application with given name security domain name reference. The name is used as the archive name too.
     */
    private static WebArchive createWar(final String sd) {
        return ShrinkWrap.create(WebArchive.class, sd + ".war").addClasses(CheckIdentityPermissionServlet.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(sd), "jboss-web.xml")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new ElytronPermission("*")), "permissions.xml")
                .addAsManifestResource(
                        Utils.getJBossDeploymentStructure("org.wildfly.extension.batch.jberet",
                                "org.wildfly.transaction.client", "org.jboss.ejb-client", "org.joda.time"),
                        "jboss-deployment-structure.xml")
                .addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml");
    }

    /**
     * Setup task which configures Elytron security domains for this test.
     */
    public static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            File moduleJar = JarUtils.createJarFile("testJar", CustomPermissionMapperImpl.class);
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine("module add --name=" + CUSTOM_PERMISSION_MAPPER_MODULE_NAME
                    + " --slot=main --dependencies=org.wildfly.security.elytron,org.wildfly.extension.elytron --resources="
                    + moduleJar.getAbsolutePath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            super.setup(modelControllerClient);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            addSecurityDomain(elements, SD_ALL, true);

            addSecurityDomain(elements, SD_NO, false);

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        private void addSecurityDomain(List<ConfigurableElement> elements, String sdName, boolean allPermissions) {
            elements.add(new CustomPermissionMappers(
                String.format("%s:add(class-name=%s, module=%s, configuration={allPermissions=%s})", sdName,
                    CustomPermissionMapperImpl.class.getName(), CUSTOM_PERMISSION_MAPPER_MODULE_NAME, allPermissions)));

            elements.add(
                    SimpleSecurityDomain.builder().withName(sdName)
                            .withDefaultRealm("ApplicationFsRealm").withPermissionMapper(sdName).withRealms(SecurityDomainRealm
                                    .builder().withRealm("ApplicationFsRealm").withRoleDecoder("groups-to-roles").build())
                            .build());
            elements.add(UndertowDomainMapper.builder().withName(sdName).withApplicationDomains(sdName).build());
        }
    }

    public static class CustomPermissionMappers implements ConfigurableElement {

        private final String[] customPermissionMappers;

        public CustomPermissionMappers(String... customPrincipalDecoders) {
            this.customPermissionMappers = customPrincipalDecoders;
        }

        @Override
        public void create(CLIWrapper cli) throws Exception {
            for (String custom : customPermissionMappers) {
                cli.sendLine("/subsystem=elytron/custom-permission-mapper=" + custom);
            }
        }

        @Override
        public void remove(CLIWrapper cli) throws Exception {
            for (String custom : customPermissionMappers) {
                int opIdx = custom.indexOf(':');
                String newPfx = custom.substring(0, opIdx + 1) + "remove()";
                cli.sendLine("/subsystem=elytron/custom-permission-mapper=" + newPfx);
            }
        }

        @Override
        public String getName() {
            return "custom-permission-mapper";
        }
    }
}
