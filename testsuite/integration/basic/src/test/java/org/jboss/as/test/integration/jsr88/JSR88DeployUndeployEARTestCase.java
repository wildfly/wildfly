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
package org.jboss.as.test.integration.jsr88;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.jar.JarOutputStream;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressObject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.jsr88.spi.DeploymentMetaData;
import org.jboss.as.jsr88.spi.JarUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Deployment API JSR-88 tests
 *
 * [AS7-3474] JSR88 undeployment does not work
 *
 * @author Thomas.Diesler@jboss.com
 * @since 01-Feb-2012
 */
@RunAsClient
@RunWith(Arquillian.class)
public class JSR88DeployUndeployEARTestCase extends AbstractDeploymentTest {

    private static final String WAR_JBOSS_FILE = "jboss-web.xml";
    private static final String JAR_JBOSS_FILE = "jboss.xml";
    private static final String EAR_JBOSS_FILE = "jboss-app.xml";

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> fakeDeployment() {
        return ShrinkWrap.create(JavaArchive.class);
    }

    @Test
    public void testDeployUndeployEAR() throws Exception {

        // [AS7-3474] JSR88 undeployment does not work

        DeploymentManager manager = getDeploymentManager(managementClient);
        try {
            ProgressObject progress = jsr88Deploy(manager, getEarArchive());

            DeploymentStatus state = progress.getDeploymentStatus();
            assertEquals(StateType.COMPLETED, state.getState());
            assertServletAccess("custom-context");

            Target[] targets = manager.getTargets();
            TargetModuleID[] targetModules = manager.getAvailableModules(ModuleType.EAR, targets);
            assertEquals(1, targetModules.length);

            jsr88Undeploy(manager, targetModules);

            try {
                assertServletAccess("custom-context");
                fail("Test deployment not undeployed");
            } catch (Exception e) {
                // ignore
            }
        } finally {
            manager.release();
        }
    }

    private void assertServletAccess(String context) throws IOException {
        // Check that we can access the servlet
        URL servletURL = new URL(managementClient.getWebUri() + "/" + context);
        BufferedReader br = new BufferedReader(new InputStreamReader(servletURL.openStream()));
        String message = br.readLine();
        assertEquals("Hello World!", message);
    }

    protected InputStream createDeploymentPlan(String deploymentFile) throws Exception {

        boolean webInf = false;
        String jbossDescriptorName = null;
        if (deploymentFile.endsWith(".war")) {
            jbossDescriptorName = WAR_JBOSS_FILE;
            webInf = true;
        } else if (deploymentFile.endsWith(".jar")) {
            jbossDescriptorName = JAR_JBOSS_FILE;
        } else if (deploymentFile.endsWith(".ear")) {
            jbossDescriptorName = EAR_JBOSS_FILE;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream plan = new JarOutputStream(baos);

        URL descriptorURL = getClass().getClassLoader().getResource(JSR88DeployUndeployEARTestCase.class.getPackage().getName().replace(".", "/") + "/" + jbossDescriptorName);
        File jbossDescriptorFile = new File(descriptorURL.getPath());
        JarUtils.addJarEntry(plan, "!/" + (webInf ? "WEB-INF/" : "META-INF/") + jbossDescriptorName, new FileInputStream(jbossDescriptorFile));

        // Setup deployment plan meta data with propriatary descriptor
        DeploymentMetaData metaData = new DeploymentMetaData(deploymentFile);
        metaData.addEntry(deploymentFile, jbossDescriptorName);

        // Add the meta data to the deployment plan
        String metaStr = metaData.toXMLString();
        JarUtils.addJarEntry(plan, DeploymentMetaData.ENTRY_NAME, new ByteArrayInputStream(metaStr.getBytes()));

        plan.flush();
        plan.close();

        return new ByteArrayInputStream(baos.toByteArray());
    }

    private Archive<?> getWebArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "deployment-web.war");
        archive.addClasses(SampleServlet.class);
        archive.addAsWebInfResource(JSR88DeployUndeployEARTestCase.class.getPackage(), "web.xml", "web.xml");
        return archive;
    }

    private Archive<?> getEjbArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "deployment-ejb.jar");
        archive.addClasses(Echo.class, EchoHome.class, EchoBean.class);
        archive.addAsManifestResource(JSR88DeployUndeployEARTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return archive;
    }

    private Archive<?> getEarArchive() {
        EnterpriseArchive archive = ShrinkWrap.create(EnterpriseArchive.class, "deployment-app.ear");
        archive.setApplicationXML(JSR88DeployUndeployEARTestCase.class.getPackage(), "application.xml");
        archive.add(getWebArchive(), "/", ZipExporter.class);
        archive.add(getEjbArchive(), "/", ZipExporter.class);
        return archive;
    }
}
