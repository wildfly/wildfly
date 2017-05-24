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

package org.wildfly.test.integration.elytron.principaldecoders;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
import org.jboss.as.test.shared.util.JarUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantPrincipalTransformer;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain.SecurityDomainRealm;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;
import org.wildfly.test.security.servlets.SecuredPrincipalPrintingServlet;

/**
 * Test for 'custom-principal-decoder' Elytron resource. Custom implementation has same behavior as constant-principal-decoder.
 * It tests if it's correctly used for authentication and remains valid as authenticated principal. It also checks for handling
 * special characters and chaining with a principal transformer.
 *
 * @author Josef Cacek
 * @author Hynek Švábek <hsvabek@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ CustomPrincipalDecoderTestCase.ServerSetup.class })
public class CustomPrincipalDecoderTestCase {

    private static final String CUSTOM_PRINCIPAL_DECODER_MODULE_NAME = "org.jboss.customprincipaldecoderimpl";

    private static final String STR_SYMBOLS = "@!#?$%^&*()%+-{}";
    private static final String STR_CHINESE = "用戶名";
    private static final String STR_ARABIC = "اسمالمستخدم";
    private static final String STR_EURO_LOWER = "žščřžďťňäáéěëíýóůúü";
    private static final String STR_EURO_UPPER = "ŽŠČŘŽĎŤŇÄÁÉĚËÍÝÓŮÚÜ";

    private static final String NAME = CustomPrincipalDecoderTestCase.class.getSimpleName();
    private static final String CONST_ADMIN = "admin";
    private static final String CONST_WHITESPACE = "two words";
    private static final String CONST_I18N = STR_SYMBOLS + STR_CHINESE + STR_ARABIC + STR_EURO_LOWER + STR_EURO_UPPER;
    private static final String PD_NAME_ADMIN = CONST_ADMIN;
    private static final String PD_NAME_I18N = "i18n";
    private static final String PD_NAME_WHITESPACE = "whitespace";
    private static final String SD_NAME_PD_AND_PT = "decoder-and-transformer";

    @Deployment(testable = false, name = PD_NAME_ADMIN)
    public static WebArchive deployment1() {
        return createWar(PD_NAME_ADMIN);
    }

    @Deployment(testable = false, name = PD_NAME_I18N)
    public static WebArchive deployment2() {
        return createWar(PD_NAME_I18N);
    }

    @Deployment(testable = false, name = PD_NAME_WHITESPACE)
    public static WebArchive deployment3() {
        return createWar(PD_NAME_WHITESPACE);
    }

    @Deployment(testable = false, name = SD_NAME_PD_AND_PT)
    public static WebArchive deployment6() {
        return createWar(SD_NAME_PD_AND_PT);
    }

    /**
     * Tests simple "admin" constant used in the custom-principal-decoder.
     */
    @Test
    @OperateOnDeployment(PD_NAME_ADMIN)
    public void testAdminPrincipalDecoder(@ArquillianResource URL url) throws Exception {
        assertEquals("Response body is not correct.", CONST_ADMIN,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_ADMIN, CONST_ADMIN, SC_OK));
        Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_WHITESPACE, CONST_WHITESPACE, SC_UNAUTHORIZED);
        assertEquals("Response body is not correct.", CONST_ADMIN,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_WHITESPACE, CONST_ADMIN, SC_OK));
        Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_I18N, CONST_I18N, SC_UNAUTHORIZED);
        assertEquals("Response body is not correct.", CONST_ADMIN,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_I18N, CONST_ADMIN, SC_OK));
    }

    /**
     * Tests constant with a whitespace used in the custom-principal-decoder.
     */
    @Test
    @OperateOnDeployment(PD_NAME_WHITESPACE)
    public void testWhitespacePrincipalDecoder(@ArquillianResource URL url) throws Exception {
        Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_ADMIN, CONST_ADMIN, SC_UNAUTHORIZED);
        assertEquals("Response body is not correct.", CONST_WHITESPACE,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_ADMIN, CONST_WHITESPACE, SC_OK));
        assertEquals("Response body is not correct.", CONST_WHITESPACE,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_WHITESPACE, CONST_WHITESPACE, SC_OK));
        Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_I18N, CONST_I18N, SC_UNAUTHORIZED);
        assertEquals("Response body is not correct.", CONST_WHITESPACE,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_I18N, CONST_WHITESPACE, SC_OK));
    }

    /**
     * Tests i18n constant used in the custom-principal-decoder.
     */
    @Test
    @OperateOnDeployment(PD_NAME_I18N)
    @Ignore("ELY-1046")
    public void testI18NPrincipalDecoder(@ArquillianResource URL url) throws Exception {
        Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_ADMIN, CONST_ADMIN, SC_UNAUTHORIZED);
        assertEquals("Response body is not correct.", CONST_I18N,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_ADMIN, CONST_I18N, SC_OK));
        Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_WHITESPACE, CONST_WHITESPACE, SC_UNAUTHORIZED);
        assertEquals("Response body is not correct.", CONST_I18N,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_WHITESPACE, CONST_I18N, SC_OK));
        assertEquals("Response body is not correct.", CONST_I18N,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_I18N, CONST_I18N, SC_OK));
    }

    /**
     * Tests principal-decoder chained with principal-transformer in a security domain.
     */
    @Test
    @OperateOnDeployment(SD_NAME_PD_AND_PT)
    public void testPrincipalDecoderAndTransformer(@ArquillianResource URL url) throws Exception {
        assertEquals("Response body is not correct.", CONST_WHITESPACE,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_ADMIN, CONST_ADMIN, SC_OK));
        Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_WHITESPACE, CONST_WHITESPACE, SC_UNAUTHORIZED);
        assertEquals("Response body is not correct.", CONST_WHITESPACE,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_WHITESPACE, CONST_ADMIN, SC_OK));
        Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_I18N, CONST_I18N, SC_UNAUTHORIZED);
        assertEquals("Response body is not correct.", CONST_WHITESPACE,
                Utils.makeCallWithBasicAuthn(principalServlet(url), CONST_I18N, CONST_ADMIN, SC_OK));
    }

    /**
     * Converts webapp URL to URL containing the included servlet path.
     */
    private URL principalServlet(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm() + SecuredPrincipalPrintingServlet.SERVLET_PATH.substring(1));
    }

    /**
     * Creates web application with given name security domain name reference. The name is used as the archive name too.
     */
    private static WebArchive createWar(final String sd) {
        return ShrinkWrap.create(WebArchive.class, sd + ".war").addClasses(SecuredPrincipalPrintingServlet.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(sd), "jboss-web.xml")
                .addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml");
    }

    /**
     * Setup task which configures Elytron security domains for this test.
     */
    public static class ServerSetup extends AbstractElytronSetupTask {
        private static final String DEFAULT_PERMISSION_MAPPER = "default-permission-mapper";
        private static final SecurityDomainRealm SD_REALM_REF = SecurityDomainRealm.builder().withRoleDecoder("groups-to-roles")
                .withRealm(NAME).build();

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            File moduleJar = JarUtils.createJarFile("testJar", CustomPrincipalDecoderImpl.class);
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine("module add --name=" + CUSTOM_PRINCIPAL_DECODER_MODULE_NAME
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
            elements.add(PropertiesRealm.builder().withName(NAME)
                    .withUser(CONST_ADMIN, CONST_ADMIN, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                    .withUser(CONST_I18N, CONST_I18N, SecuredPrincipalPrintingServlet.ALLOWED_ROLE)
                    .withUser(CONST_WHITESPACE, CONST_WHITESPACE, SecuredPrincipalPrintingServlet.ALLOWED_ROLE).build());
            addSecurityDomainWithPermissionMapper(elements, PD_NAME_ADMIN, CONST_ADMIN);
            addSecurityDomainWithPermissionMapper(elements, PD_NAME_WHITESPACE, CONST_WHITESPACE);
            addSecurityDomainWithPermissionMapper(elements, PD_NAME_I18N, CONST_I18N);

            elements.add(ConstantPrincipalTransformer.builder().withName(CONST_ADMIN).withConstant(CONST_ADMIN).build());
            elements.add(SimpleSecurityDomain.builder().withName(SD_NAME_PD_AND_PT).withDefaultRealm(NAME)
                    .withRealms(SD_REALM_REF).withPrincipalDecoder(PD_NAME_WHITESPACE)
                    .withPostRealmPrincipalTransformer(CONST_ADMIN).withPermissionMapper(DEFAULT_PERMISSION_MAPPER).build());
            elements.add(UndertowDomainMapper.builder().withName(SD_NAME_PD_AND_PT).withApplicationDomains(SD_NAME_PD_AND_PT)
                    .build());
            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        private void addSecurityDomainWithPermissionMapper(List<ConfigurableElement> elements, String pdName,
            String pdCustom) {
            elements.add(new CustomPrincipalDecoders(
                String.format("%s:add(class-name=%s, module=%s, configuration={decodeTo=%s})", pdName,
                    CustomPrincipalDecoderImpl.class.getName(), CUSTOM_PRINCIPAL_DECODER_MODULE_NAME, pdCustom)));

            elements.add(SimpleSecurityDomain.builder().withName(pdName).withPrincipalDecoder(pdName).withDefaultRealm(NAME)
                    .withRealms(SD_REALM_REF).withPermissionMapper(DEFAULT_PERMISSION_MAPPER).build());
            elements.add(UndertowDomainMapper.builder().withName(pdName).withApplicationDomains(pdName).build());
        }

        public static class CustomPrincipalDecoders implements ConfigurableElement {

            private final String[] customPrincipalDecoders;

            public CustomPrincipalDecoders(String... customPrincipalDecoders) {
                this.customPrincipalDecoders = customPrincipalDecoders;
            }

            @Override
            public void create(CLIWrapper cli) throws Exception {
                for (String custom : customPrincipalDecoders) {
                    cli.sendLine("/subsystem=elytron/custom-principal-decoder=" + custom);
                }
            }

            @Override
            public void remove(CLIWrapper cli) throws Exception {
                for (String custom : customPrincipalDecoders) {
                    int opIdx = custom.indexOf(':');
                    String newPfx = custom.substring(0, opIdx + 1) + "remove()";
                    cli.sendLine("/subsystem=elytron/custom-principal-decoder=" + newPfx);
                }
            }

            @Override
            public String getName() {
                return "custom-principal-decoder";
            }
        }
    }
}
