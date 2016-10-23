/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.loginmodules;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import javax.security.auth.AuthPermission;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
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
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.loginmodules.common.CustomEjbAccessingLoginModule;
import org.jboss.as.test.integration.security.loginmodules.common.SimpleSecuredEJB;
import org.jboss.as.test.integration.security.loginmodules.common.SimpleSecuredEJBImpl;
import org.jboss.dmr.ModelNode;
import org.jboss.security.auth.spi.RunAsLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * This is a test case for RunAsLoginModule
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RunAsLoginModuleTestCase.SecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class RunAsLoginModuleTestCase {

    public static class SecurityDomainSetup extends AbstractSecurityDomainSetup {

        protected String getSecurityDomainName() {
            return "RunAsLoginModuleTest";
        }

        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode updates = compositeOp.get(STEPS);
            PathAddress address = PathAddress.pathAddress()
                    .append(SUBSYSTEM, "security")
                    .append(SECURITY_DOMAIN, getSecurityDomainName());

            updates.add(Util.createAddOperation(address));
            address = address.append(Constants.AUTHENTICATION, Constants.CLASSIC);
            updates.add(Util.createAddOperation(address));

            ModelNode loginModule = Util.createAddOperation(address.append(LOGIN_MODULE, RunAsLoginModule.class.getName()));
            loginModule.get(CODE).set(RunAsLoginModule.class.getName());
            loginModule.get(FLAG).set("optional");
            ModelNode moduleOptions = loginModule.get("module-options");
            moduleOptions.get("roleName").set("RunAsLoginModuleRole");

            ModelNode loginModule2 = Util.createAddOperation(address.append(LOGIN_MODULE, CustomEjbAccessingLoginModule.class.getName()));
            loginModule2.get(CODE).set(CustomEjbAccessingLoginModule.class.getName());
            loginModule2.get(FLAG).set("required");

            loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            loginModule2.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            updates.add(loginModule);
            updates.add(loginModule2);

            applyUpdates(managementClient.getControllerClient(), Arrays.asList(compositeOp));

        }
    }


    private static final String DEP1 = "RunAsLoginModule";

    /**
     * Test deployment
     */
    @Deployment(name = DEP1, order = 1)
    public static WebArchive appDeployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEP1 + ".war");
        war.addClass(PrincipalPrintingServlet.class);
        war.setWebXML(Utils.getResource("org/jboss/as/test/integration/security/loginmodules/deployments/RunAsLoginModule/web.xml"));
        war.addAsWebInfResource(Utils.getResource("org/jboss/as/test/integration/security/loginmodules/deployments/RunAsLoginModule/jboss-web.xml"), "jboss-web.xml");

        war.addClasses(SimpleSecuredEJB.class, SimpleSecuredEJBImpl.class, CustomEjbAccessingLoginModule.class);
        war.addAsManifestResource(createPermissionsXmlAsset(new AuthPermission("modifyPrincipals")), "permissions.xml");

        return war;
    }

    /**
     * Correct login
     *
     * @throws Exception
     */
    @OperateOnDeployment(DEP1)
    @Test
    public void testCleartextPassword1(@ArquillianResource URL url) throws Exception {
        HttpResponse response;

        HttpGet httpget = new HttpGet(url.toString());
        String headerValue = Base64.getEncoder().encodeToString("anil:anil".getBytes());
        Assert.assertNotNull(headerValue);
        httpget.addHeader("Authorization", "Basic " + headerValue);
        String text;

        try (CloseableHttpClient httpclient = HttpClients.createDefault()){
            response = httpclient.execute(httpget);
            text = Utils.getContent(response);
        } catch (IOException e) {
            throw new RuntimeException("Servlet response IO exception", e);
        }

        assertTrue("An unexpected response: " + text, text.contains("anil"));
    }

}
