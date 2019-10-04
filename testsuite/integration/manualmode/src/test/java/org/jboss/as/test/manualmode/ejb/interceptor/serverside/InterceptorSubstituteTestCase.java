/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.ejb.interceptor.serverside;

import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;
import static org.jboss.as.controller.client.helpers.ClientConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.AbstractServerInterceptorsSetupTask;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.InterceptorModule;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test case substitutes existing interceptor module with a newer one and checks its availability.
 * See https://issues.jboss.org/browse/WFLY-6143 for more details.
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(InterceptorSubstituteTestCase.SetupTask.class)
@RunAsClient
public class InterceptorSubstituteTestCase {

    private static final Logger log = Logger.getLogger(InterceptorSubstituteTestCase.class);

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private ManagementClient managementClient;

    private static Context ctx;
    private static final String DEPLOYMENT = "server-interceptor-substitute";
    private static final String CONTAINER = "jbossas-non-clustered";

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar");
        jar.addPackage(InterceptorSubstituteTestCase.class.getPackage());
        jar.addPackage(AbstractServerInterceptorsSetupTask.class.getPackage());
        return jar;
    }

    @Before
    public void before() throws Exception {
        final Properties jndiProperties = new Properties();
        jndiProperties.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        jndiProperties.putAll(getEJBClientProperties());
        ctx = new InitialContext(jndiProperties);

        controller.start(CONTAINER);
        log.trace("===Appserver started===");
        deployer.deploy(DEPLOYMENT);
        log.trace("===Deployment deployed===");
    }

    @After
    public void after() throws Exception {
        ctx.close();

        try {
            if (!controller.isStarted(CONTAINER)) {
                controller.start(CONTAINER);
            }
            deployer.undeploy(DEPLOYMENT);
            log.trace("===Deployment undeployed===");
        } finally {
            controller.stop(CONTAINER);
            log.trace("===Appserver stopped===");
        }
    }

    /**
     * Create a module with a newer interceptor module and substitute a deployed one. Server restart is needed.
     */
    @Test
    public void substituteInterceptorModule() throws Exception {
        SubstituteSampleBeanRemote bean = (SubstituteSampleBeanRemote) ctx.lookup("ejb:" + "" + "/" + DEPLOYMENT + "/" + "" + "/" +
                SubstituteSampleBean.class.getSimpleName() + "!" + SubstituteSampleBeanRemote.class.getName());

        Assert.assertNotNull(bean);
        Assert.assertEquals(SubstituteInterceptor.PREFIX + SubstituteSampleBean.class.getSimpleName(), bean.getSimpleName());

        // on Windows machines module jar is locked by a process,
        // so in order to replace the module we need to restart the server
        controller.stop(CONTAINER);
        log.trace("===appserver stopped===");

        InterceptorModule updatedInterceptorModule = createUpdatedInterceptorModule();

        controller.start(CONTAINER);
        log.trace("===appserver started again===");

        modifyServerInterceptorsAttribute(updatedInterceptorModule);

        // "server-interceptors" attribute changing requires server restart
        controller.stop(CONTAINER);
        log.trace("===appserver stopped===");

        controller.start(CONTAINER);
        log.trace("===appserver started again===");

        try {
            SubstituteSampleBeanRemote bean2 = (SubstituteSampleBeanRemote) ctx.lookup("ejb:" + "" + "/" + DEPLOYMENT + "/" + "" + "/" +
                    SubstituteSampleBean.class.getSimpleName() + "!" + SubstituteSampleBeanRemote.class.getName());
            Assert.assertNotNull(bean2);
            String beanName = bean2.getSimpleName();
            Assert.assertEquals(UpdatedInterceptor.PREFIX + SubstituteSampleBean.class.getSimpleName(), beanName);
        } finally {
            reverServerInterceptorsAttribute();

            // "server-interceptors" attribute changing requires server restart
            controller.stop(CONTAINER);
            log.trace("===appserver stopped===");

            // on Windows machines module jar is locked by a process,
            // so in order to remove the module we need to have the server stopped
            updatedInterceptorModule.getTestModule().remove();
        }
    }

    private InterceptorModule createUpdatedInterceptorModule() {
        InterceptorModule updatedInterceptorModule = new InterceptorModule(
                UpdatedInterceptor.class,
                "interceptor-module",
                "updated-module.xml",
                InterceptorSubstituteTestCase.class.getResource("updated-module.xml"),
                "server-side-interceptor-updated.jar"
        );

        try {
            URL url = updatedInterceptorModule.getModuleXmlPath();
            if (url == null) {
                throw new IllegalStateException("Could not find " + updatedInterceptorModule.getModuleXmlName());
            }
            File moduleXmlFile = new File(url.toURI());
            updatedInterceptorModule.setTestModule(new TestModule(updatedInterceptorModule.getModuleName(), moduleXmlFile));
            JavaArchive jar = updatedInterceptorModule.getTestModule().addResource(updatedInterceptorModule.getJarName());
            jar.addClass(updatedInterceptorModule.getInterceptorClass());
            updatedInterceptorModule.getTestModule().create(true);
        } catch (Exception e) {
            throw new RuntimeException("An error while creating interceptor module", e);
        }
        return updatedInterceptorModule;
    }

    private void modifyServerInterceptorsAttribute(InterceptorModule updatedInterceptorModule) throws IOException {
        final ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(SUBSYSTEM, "ejb3");
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("server-interceptors");

        final ModelNode value = new ModelNode();

        ModelNode node = new ModelNode();
        node.get(MODULE).set(updatedInterceptorModule.getModuleName());
        node.get("class").set(updatedInterceptorModule.getInterceptorClass().getName());
        value.add(node);

        op.get(VALUE).set(value);
        managementClient.getControllerClient().execute(op);
    }

    private void reverServerInterceptorsAttribute() throws IOException {
        final ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(SUBSYSTEM, "ejb3");
        op.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("server-interceptors");

        final ModelNode operationResult = managementClient.getControllerClient().execute(op);
        // check whether the operation was successful
        assertTrue(Operations.isSuccessfulOutcome(operationResult));
    }

    private static Properties getEJBClientProperties() throws Exception {
        final InputStream inputStream = InterceptorSubstituteTestCase.class.getResourceAsStream("jboss-ejb-client.properties");
        if (inputStream == null) {
            throw new IllegalStateException("Could not find jboss-ejb-client.properties in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

    static class SetupTask extends AbstractServerInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                    SubstituteInterceptor.class,
                    "interceptor-module",
                    "module.xml",
                    InterceptorSubstituteTestCase.class.getResource("module.xml"),
                    "server-side-interceptor-substitute.jar"
                    )
            );
        }
    }

}
