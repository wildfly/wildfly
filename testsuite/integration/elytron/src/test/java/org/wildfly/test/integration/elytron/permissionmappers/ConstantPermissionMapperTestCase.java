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
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.ejb.client.RemoteEJBPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.joda.time.JodaTimePermission;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.batch.jberet.deployment.BatchPermission;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.security.permission.NoPermission;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantPermissionMapper;
import org.wildfly.test.security.common.elytron.PermissionRef;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain.SecurityDomainRealm;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;
import org.wildfly.test.security.servlets.CheckIdentityPermissionServlet;
import org.wildfly.transaction.client.RemoteTransactionPermission;

/**
 * Test for "constant-permission-mapper" Elytron resource. It tests if the defined permissions are correctly mapped to users.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ ConstantPermissionMapperTestCase.ServerSetup.class })
public class ConstantPermissionMapperTestCase {

    private static final String SD_DEFAULT = "other";
    private static final String SD_NO_MAPPER = "no-permission-mapper";
    private static final String SD_LOGIN = "login-permission";
    private static final String SD_ALL = "all-permission";
    private static final String SD_MODULES = "permissions-in-modules";

    private static final String TARGET_NAME = "name";
    private static final String TARGET_NAME_START = "start";
    private static final String ACTION = "action";

    @Deployment(testable = false, name = SD_NO_MAPPER)
    public static WebArchive deployment1() {
        return createWar(SD_NO_MAPPER);
    }

    @Deployment(testable = false, name = SD_DEFAULT)
    public static WebArchive deployment2() {
        return createWar(SD_DEFAULT);
    }

    @Deployment(testable = false, name = SD_LOGIN)
    public static WebArchive deployment3() {
        return createWar(SD_LOGIN);
    }

    @Deployment(testable = false, name = SD_ALL)
    public static WebArchive deployment4() {
        return createWar(SD_ALL);
    }

    @Deployment(testable = false, name = SD_MODULES)
    public static WebArchive deployment5() {
        return createWar(SD_MODULES);
    }

    /**
     * Tests default security domain permissions.
     */
    @Test
    @OperateOnDeployment(SD_DEFAULT)
    public void testDefaultDomainPermissions(@ArquillianResource URL url) throws Exception {
        // anonymous
        assertUserHasntPermission(url, null, null, AllPermission.class.getName(), null, null);
        // WFCORE-2666
        assertUserHasntPermission(url, null, null, LoginPermission.class.getName(), null, null);
        assertUserHasPermission(url, null, null, BatchPermission.class.getName(), TARGET_NAME_START, null);
        assertUserHasPermission(url, null, null, RemoteTransactionPermission.class.getName(), null, null);
        assertUserHasPermission(url, null, null, RemoteEJBPermission.class.getName(), null, null);
        // valid user
        assertUserHasPermission(url, "guest", "guest", LoginPermission.class.getName(), null, null);
        assertUserHasPermission(url, "guest", "guest", BatchPermission.class.getName(), TARGET_NAME_START, null);
        assertUserHasPermission(url, "guest", "guest", RemoteTransactionPermission.class.getName(), null, null);
        assertUserHasPermission(url, "guest", "guest", RemoteEJBPermission.class.getName(), null, null);
    }

    /**
     * Tests security domain which doesn't contain any permission-mapper.
     */
    @Test
    @OperateOnDeployment(SD_NO_MAPPER)
    public void testNoMapper(@ArquillianResource URL url) throws Exception {
        // anonymous
        assertUserHasntPermission(url, null, null, AllPermission.class.getName(), null, null);
        assertUserHasntPermission(url, null, null, LoginPermission.class.getName(), null, null);
        // valid user
        assertUserHasntPermission(url, "guest", "guest", LoginPermission.class.getName(), null, null);
        assertUserHasntPermission(url, "guest", "guest", BatchPermission.class.getName(), TARGET_NAME_START, null);
    }

    /**
     * Tests security domain which contains constant-permission-mapper with AllPermission.
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
     * Tests security domain which contains constant-permission-mapper with LoginPermission.
     */
    @Test
    @OperateOnDeployment(SD_LOGIN)
    public void testLoginPermission(@ArquillianResource URL url) throws Exception {
        // anonymous
        assertUserHasntPermission(url, null, null, AllPermission.class.getName(), null, null);
        assertUserHasPermission(url, null, null, LoginPermission.class.getName(), null, null);
        // login permission doesn't take into account the name and action
        assertUserHasPermission(url, null, null, LoginPermission.class.getName(), TARGET_NAME, ACTION);

        // valid user
        assertUserHasPermission(url, "guest", "guest", LoginPermission.class.getName(), null, null);
        assertUserHasntPermission(url, "guest", "guest", BatchPermission.class.getName(), TARGET_NAME_START, null);
        assertUserHasntPermission(url, "guest", "guest", NoPermission.class.getName(), null, null);
    }

    /**
     * Tests security domain which contains constant-permission-mapper with permissions from different modules
     */
    @Test
    @OperateOnDeployment(SD_MODULES)
    public void testPermissionsInModules(@ArquillianResource URL url) throws Exception {
        // anonymous
        assertUserHasntPermission(url, null, null, AllPermission.class.getName(), null, null);
        assertUserHasPermission(url, null, null, LoginPermission.class.getName(), null, null);
        assertUserHasntPermission(url, null, null, BatchPermission.class.getName(), "stop", null);
        assertUserHasPermission(url, null, null, BatchPermission.class.getName(), TARGET_NAME_START, null);
        assertUserHasPermission(url, null, null, JodaTimePermission.class.getName(), TARGET_NAME, null);

        // valid user
        assertUserHasPermission(url, "guest", "guest", LoginPermission.class.getName(), null, null);
        assertUserHasPermission(url, "guest", "guest", BatchPermission.class.getName(), TARGET_NAME_START, null);
        assertUserHasPermission(url, "guest", "guest", JodaTimePermission.class.getName(), TARGET_NAME, null);
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
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(post)) {
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
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new ElytronPermission("authenticate"),
                        new ElytronPermission("getSecurityDomain")
                ), "permissions.xml")
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
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(
                    SimpleSecurityDomain.builder()
                            .withName(SD_NO_MAPPER).withDefaultRealm("ApplicationRealm").withRealms(SecurityDomainRealm
                                    .builder().withRealm("ApplicationRealm").withRoleDecoder("groups-to-roles").build())
                            .build());
            elements.add(UndertowDomainMapper.builder().withName(SD_NO_MAPPER).withApplicationDomains(SD_NO_MAPPER).build());

            addSecurityDomain(elements, SD_ALL, PermissionRef.fromPermission(new AllPermission()));
            addSecurityDomain(elements, SD_LOGIN, PermissionRef.fromPermission(new LoginPermission(TARGET_NAME, ACTION)));
            addSecurityDomain(elements, SD_MODULES, PermissionRef.fromPermission(new LoginPermission()),
                    PermissionRef.fromPermission(new BatchPermission(TARGET_NAME_START), "org.wildfly.extension.batch.jberet"),
                    PermissionRef.fromPermission(new JodaTimePermission(TARGET_NAME), "org.joda.time"));

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        private void addSecurityDomain(List<ConfigurableElement> elements, String sdName, PermissionRef... permRefs) {
            elements.add(ConstantPermissionMapper.builder().withName(sdName).withPermissions(permRefs).build());
            elements.add(
                    SimpleSecurityDomain.builder().withName(sdName)
                            .withDefaultRealm("ApplicationFsRealm").withPermissionMapper(sdName).withRealms(SecurityDomainRealm
                                    .builder().withRealm("ApplicationFsRealm").withRoleDecoder("groups-to-roles").build())
                            .build());
            elements.add(UndertowDomainMapper.builder().withName(sdName).withApplicationDomains(sdName).build());
        }
    }
}
