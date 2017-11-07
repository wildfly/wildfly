/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.auditing;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.MODULE;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.integration.security.loginmodules.common.CustomLoginModule1;
import org.jboss.as.test.integration.security.loginmodules.common.CustomLoginModule2;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.undertow.util.FlexBase64;

/**
 * Tests whether the loading of an audit provider module from a non-default JBoss module works properly.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CustomAuditProviderModuleTest.CustomAuditProviderModuleSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class CustomAuditProviderModuleTest {

    /**
     * Creates two JBoss modules that host {@link CustomLoginModule1} and {@link CustomLoginModule2} respectively and then
     * creates a security domain that uses them in a chain.
     */
    static class CustomAuditProviderModuleSecurityDomainSetup extends AbstractSecurityDomainSetup {

        private static final Logger log = Logger.getLogger(CustomAuditProviderModuleSecurityDomainSetup.class);

        private static final String SECURITY_DOMAIN_NAME = "custom-audit-module-" + RANDOM_EXECUTION_ID;

        private final TestModule auditProviderJBossModule;

        private final TestModule loginJBossModule;

        public CustomAuditProviderModuleSecurityDomainSetup() {

            final Class<?> providerModuleClass = CustomAuditProviderModule.class;
            auditProviderJBossModule = new TestModule(providerModuleClass.getName()+ RANDOM_EXECUTION_ID, "org.picketbox", "javax.api",
                    "org.jboss.logging");
            JavaArchive auditJar = auditProviderJBossModule.addResource(providerModuleClass.getSimpleName() + ".jar");
            auditJar.addClass(providerModuleClass);

            Class<?> loginModuleClass = CustomLoginModule1.class;
            loginJBossModule = new TestModule(loginModuleClass.getName()+ RANDOM_EXECUTION_ID, "org.picketbox", "javax.api", "org.jboss.logging");
            JavaArchive loginJar = loginJBossModule.addResource(loginModuleClass.getSimpleName() + ".jar");
            loginJar.addClass(loginModuleClass);
        }

        @Override
        protected String getSecurityDomainName() {
            return SECURITY_DOMAIN_NAME;
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws IOException {

            auditProviderJBossModule.create(true);
            loginJBossModule.create(true);

            log.debug("start of the domain creation");

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();

            ModelNode steps = compositeOp.get(STEPS);
            PathAddress address = PathAddress.pathAddress().append(SUBSYSTEM, "security").append(SECURITY_DOMAIN,
                    getSecurityDomainName());

            steps.add(Util.createAddOperation(address));
            PathAddress authAddress = address.append(Constants.AUTHENTICATION, Constants.CLASSIC);
            steps.add(Util.createAddOperation(authAddress));

            final Class<?> loginModuleClass = CustomLoginModule1.class;
            ModelNode loginModule1 = Util
                    .createAddOperation(authAddress.append(LOGIN_MODULE, loginModuleClass.getSimpleName()));
            loginModule1.get(CODE).set(loginModuleClass.getName());
            loginModule1.get(MODULE).set(loginJBossModule.getName());
            loginModule1.get(FLAG).set("required");
            loginModule1.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            steps.add(loginModule1);


            PathAddress auditAddress = address.append(Constants.AUDIT, Constants.CLASSIC);
            steps.add(Util.createAddOperation(auditAddress));

            final Class<?> auditProviderClass = CustomAuditProviderModule.class;
            ModelNode auditProvider = Util
                    .createAddOperation(auditAddress.append(Constants.PROVIDER_MODULE, auditProviderClass.getSimpleName()));
            auditProvider.get(CODE).set(auditProviderClass.getName());
            auditProvider.get(MODULE).set(auditProviderJBossModule.getName());
            auditProvider.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            steps.add(auditProvider);


            ModelNode addAuditLogOp = Util.createAddOperation(PathAddress.pathAddress().append(SUBSYSTEM, "logging")
                    .append("periodic-rotating-file-handler", AUDIT_HANDLER_NAME));
            addAuditLogOp.get("level").set("INFO");
            addAuditLogOp.get("append").set("true");
            addAuditLogOp.get("suffix").set(".yyyy-MM-dd");
            ModelNode file = new ModelNode();
            file.get("relative-to").set("jboss.server.log.dir");
            file.get("path").set(AUDIT_LOG_FILE_NAME);
            addAuditLogOp.get("file").set(file);
            addAuditLogOp.get("formatter").set("%-5p %c %s%E%n");
            steps.add(addAuditLogOp);

            ModelNode addAuditLoggerOp = Util.createAddOperation(PathAddress.pathAddress().append(SUBSYSTEM, "logging")
                    .append("logger", CustomAuditProviderModule.class.getName()));
            addAuditLoggerOp.get("level").set("INFO");
            addAuditLoggerOp.get("handlers").add(AUDIT_HANDLER_NAME);
            steps.add(addAuditLoggerOp);
            applyUpdates(managementClient.getControllerClient(), Arrays.asList(compositeOp));

            log.debug("end of the domain creation");
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) {
            super.tearDown(managementClient, containerId);

            auditProviderJBossModule.remove();
            loginJBossModule.remove();

        }

    }

    private static final Charset UTF_8 = Charset.forName("utf-8");
    private static String RANDOM_EXECUTION_ID = String.valueOf(UUID.randomUUID().toString().replace("-", ""));
    private static final String AUDIT_HANDLER_NAME;
    private static final String AUDIT_LOG_FILE_NAME;
    private static Path AUDIT_LOG_PATH;

    static {
        /*
         * Let's make both the audit handler name and the audit log file specific for this class and execution so that we do not
         * interfere with other test classes or multiple subsequent executions of this class against the same container
         */
        AUDIT_HANDLER_NAME = "audit-" + CustomAuditProviderModuleTest.class.getSimpleName() + "-" + RANDOM_EXECUTION_ID;
        AUDIT_LOG_FILE_NAME = AUDIT_HANDLER_NAME + ".log";
        AUDIT_LOG_PATH = new File(System.getProperty("jboss.home", null),
                "standalone" + File.separator + "log" + File.separator + AUDIT_LOG_FILE_NAME).toPath();
    }

    private static void assertAuditLog(BufferedReader reader, String regex) throws Exception {

        Pattern successPattern = Pattern.compile(regex);

        // we'll be actively waiting for a given INTERVAL for the record to appear
        final long deadline = 5000 + System.currentTimeMillis();
        String line;
        while (true) {
            // some new lines were added -> go trough those and check whether our record is present
            while (null != (line = reader.readLine())) {
                if (successPattern.matcher(line).find()) {
                    return;
                }
            }
            // record not written to log yet -> continue checking if the time has not yet expired
            if (System.currentTimeMillis() > deadline) {
                // time expired
                throw new AssertionError(
                        "Login record has not been written into audit log! (time expired). Checked regex=" + regex);
            }
        }

    }

    @Deployment
    public static WebArchive deployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, CustomAuditProviderModuleTest.class.getSimpleName() + ".war");
        war.addClass(SecuredServlet.class);
        war.addAsWebInfResource(new StringAsset("<jboss-web>" + //
                "<security-domain>" + CustomAuditProviderModuleSecurityDomainSetup.SECURITY_DOMAIN_NAME + "</security-domain>" //
                + "<disable-audit>false</disable-audit>" //
                + "</jboss-web>"), "jboss-web.xml");
        war.addAsWebInfResource(new StringAsset("<web-app>" //
                + " <login-config>" //
                + " <auth-method>BASIC</auth-method>" //
                + " </login-config>" //
                + " </web-app>"), "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    private void assertResponse(String user, String password, int expectedStatusCode) throws Exception {

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

            HttpGet request = new HttpGet(url.toExternalForm() + "secured/");

            if (password != null) {
                request.addHeader(HttpHeaders.AUTHORIZATION,
                        "Basic " + FlexBase64.encodeString((user + ":" + password).getBytes("utf-8"), false));
            }

            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            StatusLine statusLine = response.getStatusLine();
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
            if (statusLine.getStatusCode() == 200) {
                String body = EntityUtils.toString(entity, "utf-8");
                assertEquals("GOOD", body);
            } else {
                EntityUtils.consume(entity);
            }
        }
    }

    @Test
    public void testBaduser1() throws Exception {
        /*
         * baduser1 can authenticate, because we use the correct password, but he does not have the "gooduser" role required by
         * SecuredServlet and the server thus return 403 rather than 401
         */
        assertResponse(CustomLoginModule1.BADUSER1_USERNAME, CustomLoginModule1.BADUSER1_PASSWORD, 403);

        try (BufferedReader r = Files.newBufferedReader(AUDIT_LOG_PATH, UTF_8)) {
            assertAuditLog(r, Pattern.quote("INFO  " + CustomAuditProviderModule.class.getName() + " [Success]principal="
                    + CustomLoginModule1.BADUSER1_USERNAME + ";"));
        }
    }

    @Test
    public void testGooduser1() throws Exception {
        assertResponse(CustomLoginModule1.GOODUSER1_USERNAME, CustomLoginModule1.GOODUSER1_PASSWORD, 200);
        try (BufferedReader r = Files.newBufferedReader(AUDIT_LOG_PATH, UTF_8)) {
            assertAuditLog(r, Pattern.quote("INFO  " + CustomAuditProviderModule.class.getName() + " [Success]principal="
                    + CustomLoginModule1.GOODUSER1_USERNAME + ";"));
        }
    }

    @Test
    public void testGooduser1WithBadPassword() throws Exception {
        assertResponse(CustomLoginModule1.GOODUSER1_USERNAME, "bogus", 401);
        try (BufferedReader r = Files.newBufferedReader(AUDIT_LOG_PATH, UTF_8)) {
            assertAuditLog(r, Pattern.quote("INFO  " + CustomAuditProviderModule.class.getName() + " [Failure]"));
        }
    }

}
