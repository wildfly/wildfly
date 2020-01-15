/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.as.testsuite.integration.secman.propertypermission;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.PropertyPermission;

import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.ee.subsystem.EeExtension;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.as.testsuite.integration.secman.servlets.JSMCheckServlet;
import org.jboss.as.testsuite.integration.secman.servlets.PrintSystemPropertyServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that enabling disabling spec-descriptor-property-replacement and jboss-descriptor-property-replacement on the EE subsystem the replacement via environment
 * properties on permissions.xml and jboss-permissions.xml is enabled and disabled.
 *
 * @author Yeray Borges
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(PropertyPermissionExpressionsTestCase.SystemPropertiesSetup.class)
public class PropertyPermissionExpressionsTestCase {
    private static final String PROPERTY_NAME = "ENVIRONMENT_PROP_NAME";
    private static final String PROPERTY_VALUE = "java.home";
    private static final PathAddress EE_SUBSYSTEM_PATH_ADDRESS = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EeExtension.SUBSYSTEM_NAME));
    private static final String VERIFY_JSM_DEPLOYMENT = "verify-jsm-deployment";
    private static final String SPEC_PRINT_PROP_SERVLET_DEPLOYMENT = "spec_print-prop-servlet_deployment";
    private static final String JBOSS_PRINT_PROP_SERVLET_DEPLOYMENT = "jboss_print-prop-servlet_deployment";
    private static final Asset EXPRESSIONS_PERMISSIONS_XML = PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(
            "${"+PROPERTY_NAME+"}", "read"));

    private ModelControllerClient client;

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private ManagementClient mgmtClient;

    @Deployment(name = VERIFY_JSM_DEPLOYMENT)
    public static Archive<?> createJavaSecManagerVerifier() {
        return ShrinkWrap.create(WebArchive.class, "verify-jsm-enabled.war")
                .addClass(JSMCheckServlet.class);
    }

    @Deployment(name = SPEC_PRINT_PROP_SERVLET_DEPLOYMENT, managed = false, testable = false)
    public static Archive<?> createSpectPrintPropServlet() {
        return ShrinkWrap.create(WebArchive.class, SPEC_PRINT_PROP_SERVLET_DEPLOYMENT + ".war")
                .addClass(PrintSystemPropertyServlet.class)
                .addAsManifestResource(EXPRESSIONS_PERMISSIONS_XML, "permissions.xml");
    }

    @Deployment(name = JBOSS_PRINT_PROP_SERVLET_DEPLOYMENT, managed = false, testable = false)
    public static Archive<?> createJbossPrintPropServlet() {
        return ShrinkWrap.create(WebArchive.class, JBOSS_PRINT_PROP_SERVLET_DEPLOYMENT + ".war")
                .addClass(PrintSystemPropertyServlet.class)
                .addAsManifestResource(EXPRESSIONS_PERMISSIONS_XML, "jboss-permissions.xml");
    }

    @Test
    @InSequence(10)
    @OperateOnDeployment(VERIFY_JSM_DEPLOYMENT)
    public void verifyJavaSecurityManageIsEnabled(@ArquillianResource() @OperateOnDeployment(VERIFY_JSM_DEPLOYMENT) URL webAppURL) throws Exception {
        final URI checkJSMuri = new URI(webAppURL.toExternalForm() + JSMCheckServlet.SERVLET_PATH.substring(1));
        assertEquals("JSM should be enabled.", Boolean.toString(true), Utils.makeCall(checkJSMuri, 200));
    }

    @Test
    @InSequence(20)
    public void verifyExpressionsInSpecPermissions() throws Exception {
        verifyExpressionsInPermissions("spec-descriptor-property-replacement", SPEC_PRINT_PROP_SERVLET_DEPLOYMENT);
    }

    @Test
    @InSequence(30)
    public void verifyExpressionsInJbossPermissions() throws Exception {
        verifyExpressionsInPermissions("jboss-descriptor-property-replacement", JBOSS_PRINT_PROP_SERVLET_DEPLOYMENT);
    }

    private void verifyExpressionsInPermissions(String eeSubsystemConfName, String deployment) throws Exception {
        final URI sysPropUri = new URI(TestSuiteEnvironment.getHttpUrl() + "/" + deployment + "/" + PrintSystemPropertyServlet.SERVLET_PATH.substring(1));
        client = mgmtClient.getControllerClient();

        executeOperation(client, Operations.createWriteAttributeOperation(EE_SUBSYSTEM_PATH_ADDRESS.toModelNode(), eeSubsystemConfName, true));
        deployer.deploy(deployment);

        Utils.makeCall(sysPropUri, HttpServletResponse.SC_OK);
        deployer.undeploy(deployment);

        executeOperation(client, Operations.createWriteAttributeOperation(EE_SUBSYSTEM_PATH_ADDRESS.toModelNode(), eeSubsystemConfName, false));
        deployer.deploy(deployment);

        Utils.makeCall(sysPropUri, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        deployer.undeploy(deployment);
    }

    protected static ModelNode executeOperation(ModelControllerClient client, ModelNode op) throws IOException {
        final ModelNode result = client.execute(op);
        Assert.assertTrue(SubsystemOperations.getFailureDescriptionAsString(result), SubsystemOperations.isSuccessfulOutcome(result));
        return result;
    }

    static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {
        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[] { new DefaultSystemProperty(PROPERTY_NAME, PROPERTY_VALUE) };
        }
    }
}
