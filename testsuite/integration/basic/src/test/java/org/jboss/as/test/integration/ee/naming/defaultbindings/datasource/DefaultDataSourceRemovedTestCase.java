/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.ee.naming.defaultbindings.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ee.subsystem.DefaultBindingsResourceDefinition;
import org.jboss.as.ee.subsystem.EESubsystemModel;
import org.jboss.as.ee.subsystem.EeExtension;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.net.SocketPermission;


/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(DefaultDataSourceRemovedTestCase.RemoveDefaultDSBindingSetupTask.class)
public class DefaultDataSourceRemovedTestCase {

    static class RemoveDefaultDSBindingSetupTask extends SnapshotRestoreSetupTask {
        @Override
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            ModelNode undefineDatasourceFromDefaultBindings = new ModelNode();
            undefineDatasourceFromDefaultBindings.get(ModelDescriptionConstants.ADDRESS)
                    .set(PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM,EeExtension.SUBSYSTEM_NAME), EESubsystemModel.DEFAULT_BINDINGS_PATH).toModelNode());
            undefineDatasourceFromDefaultBindings.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
            undefineDatasourceFromDefaultBindings.get(ModelDescriptionConstants.NAME).set(DefaultBindingsResourceDefinition.DATASOURCE);
            ModelNode response = client.getControllerClient().execute(undefineDatasourceFromDefaultBindings);
            Assert.assertEquals(SUCCESS, response.get(OUTCOME).asString());
        }
    }

    @Deployment(name = "with-default-ds")
    public static WebArchive defaultDS() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "with-default-ds.war");
        war.addClasses(DefaultDSServlet.class);
        war.addPackage(HttpRequest.class.getPackage());
        war.addAsManifestResource(createPermissionsXmlAsset(
            new RuntimePermission("modifyThread"),
            new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")), "permissions.xml");
        return war;
    }

    @Deployment(name = "with-default-ds-with-name")
    public static WebArchive defaultDSWithName() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "with-default-ds-with-name.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addClasses(DefaultDSWithNameServlet.class);
        war.addAsWebInfResource(WebXml.get("<servlet>\n" +
                "  <servlet-name>DefaultDSWithNameServlet</servlet-name>\n" +
                "  <servlet-class>org.jboss.as.test.integration.ee.naming.defaultbindings.datasource.DefaultDSWithNameServlet</servlet-class>" +
                "  <load-on-startup>1</load-on-startup>\n" +
                "</servlet>\n" +
                "<servlet-mapping>\n" +
                "    <servlet-name>DefaultDSWithNameServlet</servlet-name>\n" +
                "    <url-pattern>/defaultDSWithName</url-pattern>\n" +
                "</servlet-mapping>"), "web.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(
            new RuntimePermission("modifyThread"),
            new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")), "permissions.xml");
        return war;
    }

    @Deployment(name = "with-default-ds-with-name-and-lookup")
    public static WebArchive defaultDSWithNameAndLookup() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "with-default-ds-with-name-and-lookup.war");
        war.addClasses(DefaultDSWithNameServlet.class);
        war.addPackage(HttpRequest.class.getPackage());
        war.addAsManifestResource(createPermissionsXmlAsset(
            new RuntimePermission("modifyThread"),
            new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")), "permissions.xml");
        war.addAsWebInfResource(WebXml.get("<servlet>\n" +
                "  <servlet-name>DefaultDSWithNameServlet</servlet-name>\n" +
                "  <servlet-class>org.jboss.as.test.integration.ee.naming.defaultbindings.datasource.DefaultDSWithNameServlet</servlet-class>\n" +
                "  <load-on-startup>1</load-on-startup>\n" +
                "    <init-param>\n" +
                "      <param-name>hasLookup</param-name>\n" +
                "      <param-value>true</param-value>\n" +
                "    </init-param>\n" +
                "</servlet>\n" +
                "<servlet-mapping>\n" +
                "    <servlet-name>DefaultDSWithNameServlet</servlet-name>\n" +
                "    <url-pattern>/defaultDSWithName</url-pattern>\n" +
                "</servlet-mapping>"), "web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web>\n" +
                "    <resource-ref>\n" +
                "        <res-ref-name>ds</res-ref-name>\n" +
                "        <res-type>javax.sql.DataSource</res-type>\n" +
                "        <lookup-name>java:jboss/datasources/ExampleDS</lookup-name>\n" +
                "    </resource-ref>\n" +
                "</jboss-web>"), "jboss-web.xml");
        return war;
    }

    @Deployment(name = "with-default-ds-with-name-and-ctx")
    public static WebArchive defaultDSWithNameAndCtx() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "with-default-ds-with-name-and-ctx.war");
        war.addClasses(DefaultDSWithCtxListenerServlet.class);
        war.addPackage(HttpRequest.class.getPackage());
        war.addAsManifestResource(createPermissionsXmlAsset(
            new RuntimePermission("modifyThread"),
            new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")), "permissions.xml");
        return war;
    }

    @Test
    @OperateOnDeployment("with-default-ds")
    public void testDeployDefaultDS(@ArquillianResource URL webURL) throws Exception {
        String response = HttpRequest.get(webURL + "/defaultDS", 10, TimeUnit.SECONDS);
        Assert.assertEquals("OK", response);
    }

    @Test
    @OperateOnDeployment("with-default-ds-with-name")
    public void testDeployDefaultDSWithName(@ArquillianResource URL webURL) throws Exception {
        String response = HttpRequest.get(webURL + "/defaultDSWithName", 10, TimeUnit.SECONDS);
        Assert.assertEquals("OK", response);
    }

    @Test
    @OperateOnDeployment("with-default-ds-with-name-and-lookup")
    public void testDeployDefaultDSWithNameAndLookup(@ArquillianResource URL webURL) throws Exception {
        String response = HttpRequest.get(webURL + "/defaultDSWithName", 10, TimeUnit.SECONDS);
        Assert.assertEquals("OK", response);
    }

    @Test
    @OperateOnDeployment("with-default-ds-with-name-and-ctx")
    public void testDeployDefaultDSWithNameAndCtx(@ArquillianResource URL webURL) throws Exception {
        String response = HttpRequest.get(webURL + "/defaultDSWithCtxListener", 10, TimeUnit.SECONDS);
        Assert.assertEquals("OK", response);
    }

}
