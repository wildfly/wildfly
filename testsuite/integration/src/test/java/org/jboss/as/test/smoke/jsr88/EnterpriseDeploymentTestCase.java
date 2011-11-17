/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.smoke.jsr88;

import static org.jboss.as.arquillian.container.Authentication.PASSWORD;
import static org.jboss.as.arquillian.container.Authentication.USERNAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.ee.deployment.spi.DeploymentManagerImpl;
import org.jboss.as.ee.deployment.spi.DeploymentMetaData;
import org.jboss.as.ee.deployment.spi.JarUtils;
import org.jboss.as.ee.deployment.spi.factories.DeploymentFactoryImpl;
import org.jboss.as.test.smoke.embedded.deployment.Echo;
import org.jboss.as.test.smoke.embedded.deployment.EchoBean;
import org.jboss.as.test.smoke.embedded.deployment.EchoHome;
import org.jboss.as.test.smoke.embedded.deployment.SampleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.util.UnreachableStatementException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Deployment API JSR-88 tests
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Aug-2011
 */
@RunAsClient
@RunWith(Arquillian.class)
public class EnterpriseDeploymentTestCase {
    private static final long TIMEOUT = 10000;

    private static final String WAR_JBOSS_FILE = "WEB-INF/jboss-web.xml";
    private static final String JAR_JBOSS_FILE = "META-INF/jboss.xml";
    private static final String EAR_JBOSS_FILE = "META-INF/jboss-app.xml";

    private DeploymentManager deploymentManager;

    @Test
    public void testDeploymentManager() throws Exception {
        DeploymentManager manager = getDeploymentManager();
        assertNotNull("DeploymentManager not null", manager);
        Target target = manager.getTargets()[0];
        assertEquals("ServerDeploymentManager target", target.getDescription());
    }

    @Test
    public void testDistributeWebApp() throws Exception {
        ProgressObject progress = jsr88Deploy(getWebArchive());
        TargetModuleID[] targetModules = progress.getResultTargetModuleIDs();
        try {
            DeploymentStatus state = progress.getDeploymentStatus();
            assertEquals(StateType.COMPLETED, state.getState());
            assertServletAccess("custom-context");
        } finally {
            jsr88Undeploy(targetModules);
        }
        try {
            assertServletAccess("custom-context");
            fail("Test deployment not undeployed");
        } catch (IOException e) {
            // ignore
        }
    }

    @Test
    public void testDistributeBadWar() throws Exception {
        ProgressObject progress = jsr88Deploy(getBadWebArchive());
        TargetModuleID[] targetModules = progress.getResultTargetModuleIDs();
        try {
            DeploymentStatus state = progress.getDeploymentStatus();
            assertEquals(StateType.FAILED, state.getState());
        } finally {
            jsr88Undeploy(targetModules);
        }
    }

    @Test
		@Ignore("AS7-2631")
    public void testDistributeEjbApp() throws Exception {
        ProgressObject progress = jsr88Deploy(getEjbArchive());
        TargetModuleID[] targetModules = progress.getResultTargetModuleIDs();
        try {
            DeploymentStatus state = progress.getDeploymentStatus();
            assertEquals(StateType.COMPLETED, state.getState());
        } finally {
            jsr88Undeploy(targetModules);
        }
    }

    @Test
		@Ignore("AS7-2631")
    public void testDistributeEARApp() throws Exception {
        ProgressObject progress = jsr88Deploy(getEarArchive());
        TargetModuleID[] targetModules = progress.getResultTargetModuleIDs();
        try {
            DeploymentStatus state = progress.getDeploymentStatus();
            assertEquals(StateType.COMPLETED, state.getState());
            assertServletAccess("custom-context");
        } finally {
            jsr88Undeploy(targetModules);
        }
        try {
            assertServletAccess("custom-context");
            fail("Test deployment not undeployed");
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
		@Ignore("AS7-2631")
    public void testListAvailableModules() throws Exception {
        DeploymentManager manager = getDeploymentManager();
        Target[] targets = manager.getTargets();
        TargetModuleID[] availableModules = manager.getAvailableModules(ModuleType.EAR, targets);
        assertNull(availableModules);

        ProgressObject progress = jsr88Deploy(getEarArchive());
        TargetModuleID[] targetModules = progress.getResultTargetModuleIDs();
        try {
            availableModules = manager.getAvailableModules(ModuleType.EAR, targets);
            assertNotNull(availableModules);
            assertEquals(1, availableModules.length);

            TargetModuleID targetModuleID = availableModules[0];
            String moduleID = targetModuleID.getModuleID();
            assertTrue("Ends with deployment-app.ear", moduleID.endsWith("deployment-app.ear"));

            // [TODO] verify child modules

        } finally {
            jsr88Undeploy(targetModules);
        }
    }

    private DeploymentManager getDeploymentManager() throws Exception {
        if (deploymentManager == null) {
            DeploymentFactoryImpl.register();
            DeploymentFactoryManager dfManager = DeploymentFactoryManager.getInstance();
            DeploymentFactory[] factories = dfManager.getDeploymentFactories();
            assertTrue("DeploymentFactory available", factories.length > 0);
            String mgrURI = DeploymentManagerImpl.DEPLOYER_URI + "?targetType=as7";
            deploymentManager = factories[0].getDeploymentManager(mgrURI, USERNAME, PASSWORD);
        }
        return deploymentManager;
    }

    private ProgressObject jsr88Deploy(Archive<?> archive) throws Exception {
        // Get the deployment manager and the distribution targets
        DeploymentManager manager = getDeploymentManager();
        Target[] targets = manager.getTargets();
        assertEquals(1, targets.length);

        InputStream deploymentPlan = createDeploymentPlan(archive.getName());

        // Deploy the test archive
        InputStream inputStream = archive.as(ZipExporter.class).exportAsInputStream();
        ProgressObject progress = manager.distribute(targets, inputStream, deploymentPlan);
        StateType state = awaitCompletion(progress, TIMEOUT);

        if (state == StateType.COMPLETED) {
            progress = manager.start(progress.getResultTargetModuleIDs());
            awaitCompletion(progress, TIMEOUT);
        }

        return progress;
    }

    private ProgressObject jsr88Undeploy(TargetModuleID[] resultTargetModuleIDs) throws Exception {
        DeploymentManager manager = getDeploymentManager();
        Target[] targets = manager.getTargets();
        assertEquals(1, targets.length);

        ProgressObject progress = manager.stop(resultTargetModuleIDs);
        awaitCompletion(progress, TIMEOUT);

        progress = manager.undeploy(resultTargetModuleIDs);
        awaitCompletion(progress, TIMEOUT);

        return progress;
    }

    private StateType awaitCompletion(ProgressObject progress, long timeout) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        progress.addProgressListener(new ProgressListener() {
            public void handleProgressEvent(ProgressEvent event) {
                DeploymentStatus status = event.getDeploymentStatus();
                if (status.isCompleted() || status.isFailed()) {
                    latch.countDown();
                }
            }
        });

        final DeploymentStatus status = progress.getDeploymentStatus();
        if (status.isCompleted())
            return status.getState();

        if (latch.await(timeout, TimeUnit.MILLISECONDS) == false)
            throw new IllegalStateException("Deployment timeout: " + progress);

        return status.getState();
    }

    private void assertServletAccess(String context) throws IOException {
        // Check that we can access the servlet
        URL servletURL = new URL("http://localhost:8080/" + context);
        BufferedReader br = new BufferedReader(new InputStreamReader(servletURL.openStream()));
        String message = br.readLine();
        assertEquals("Hello World!", message);
    }

    private InputStream createDeploymentPlan(String deploymentFile) throws Exception {
        String[] strs = null;

        String jbossFile = getJBossFile(deploymentFile);
        File jbossDescriptor = getResourceFile("deployment/" + jbossFile);
        assertTrue(jbossDescriptor.exists());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos);
        JarUtils.addJarEntry(jos, "!/" + jbossFile, new FileInputStream(jbossDescriptor));

        // Setup deployment plan meta data with propriatary descriptor
        DeploymentMetaData metaData = new DeploymentMetaData(deploymentFile);

        strs = jbossFile.split("/");
        metaData.addEntry(deploymentFile, strs[strs.length - 1]);

        // Add the meta data to the deployment plan
        String metaStr = metaData.toXMLString();

        JarUtils.addJarEntry(jos, DeploymentMetaData.ENTRY_NAME, new ByteArrayInputStream(metaStr.getBytes()));
        jos.flush();
        jos.close();

        return new ByteArrayInputStream(baos.toByteArray());
    }

    private String getJBossFile(String deploymentFile) {
        if (deploymentFile.endsWith(".war"))
            return WAR_JBOSS_FILE;
        else if (deploymentFile.endsWith(".jar"))
            return JAR_JBOSS_FILE;
        else if (deploymentFile.endsWith(".ear"))
            return EAR_JBOSS_FILE;
        else
            fail("Wrong J2EE Module found...");
        throw new UnreachableStatementException();
    }

    private File getResourceFile(String resource) {
        File file = new File(resource);
        if (file.exists())
            return file;

        String testResourcesDir = System.getProperty("jbossas.ts.submodule.dir") + "/target/test-classes";
        file = new File(testResourcesDir + "/" + resource);
        if (file.exists())
            return file;

        throw new IllegalArgumentException("Cannot obtain '" + testResourcesDir + "/" + resource + "'");
    }

    private Archive<?> getWebArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "deployment-web.war");
        archive.addClasses(SampleServlet.class);
        archive.setWebXML("deployment/WEB-INF/web.xml");
        return archive;
    }

    private Archive<?> getBadWebArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "deployment-bad-web.war");
        archive.addClasses(SampleServlet.class);
        archive.setWebXML("deployment/WEB-INF/badweb.xml");
        return archive;
    }

    private Archive<?> getEjbArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "deployment-ejb.jar");
        archive.addClasses(Echo.class, EchoHome.class, EchoBean.class);
        archive.addAsManifestResource("deployment/META-INF/ejb-jar.xml", "ejb-jar.xml");
        return archive;
    }

    private Archive<?> getEarArchive() {
        EnterpriseArchive archive = ShrinkWrap.create(EnterpriseArchive.class, "deployment-app.ear");
        archive.setApplicationXML("deployment/META-INF/application.xml");
        archive.add(getWebArchive(), "/", ZipExporter.class);
        archive.add(getEjbArchive(), "/", ZipExporter.class);
        return archive;
    }
}
