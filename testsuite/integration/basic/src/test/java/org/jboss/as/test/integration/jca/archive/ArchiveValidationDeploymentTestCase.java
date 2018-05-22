/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.archive;

import static org.junit.Assert.fail;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a> JBQA-6006 archive validation checking
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ArchiveValidationDeploymentTestCase.ArchiveValidationDeploymentTestCaseSetup.class)
public class ArchiveValidationDeploymentTestCase extends JcaMgmtBase {

    private static Logger log = Logger.getLogger("ArchiveValidationDeploymentTestCase");

    static class ArchiveValidationDeploymentTestCaseSetup extends JcaMgmtServerSetupTask {
        boolean enabled;
        boolean failingOnError;
        boolean failingOnWarning;

        @Override
        public void doSetup(ManagementClient managementClient) throws Exception {
            enabled = getArchiveValidationAttribute("enabled");
            failingOnError = getArchiveValidationAttribute("fail-on-error");
            failingOnWarning = getArchiveValidationAttribute("fail-on-warn");
            log.trace("//save//" + enabled + "//" + failingOnError + "//" + failingOnWarning);
        }

    }

    @ArquillianResource
    Deployer deployer;

    /**
     * Define the deployment
     *
     * create rar archive either valid or invalid where archive validation reports warning or error
     * WARNING: triggered by wrong config property type in MultipleWarningResourceAdapter, int instead of Integer (name property)
     * ERROR: triggered by missing equals and hashCode in MultipleErrorResourceAdapter
     *
     * @return The deployment archive
     */
    public static ResourceAdapterArchive createDeployment(String name) throws Exception {

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, name + ".rar");
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, name + ".jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).addClasses(ArchiveValidationDeploymentTestCase.class,
                MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class, JcaMgmtServerSetupTask.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(ArchiveValidationDeploymentTestCase.class.getPackage(), name + "ra.xml", "ra.xml")
                .addAsManifestResource(ArchiveValidationDeploymentTestCase.class.getPackage(), "ironjacamar.xml", "ironjacamar.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,org.jboss.as.connector \n"),
                        "MANIFEST.MF");
        return raa;
    }

    /*
     * this deployment is important to trigger ArchiveValidationDeploymentTestCaseSetup.doSetup() method before first test run
     */
    @Deployment(name = "fake", managed = true)
    public static Archive<?> deployment() throws Exception {
        return ShrinkWrap.create(JavaArchive.class, "fake.jar");
    }

    @Deployment(name = "ok", managed = false)
    public static ResourceAdapterArchive deployment1() throws Exception {
        return createDeployment("ok_");
    }

    @Deployment(name = "error", managed = false)
    public static ResourceAdapterArchive deployment2() throws Exception {
        return createDeployment("error_");
    }

    @Deployment(name = "warning", managed = false)
    public static ResourceAdapterArchive deployment3() throws Exception {
        return createDeployment("warning_");
    }

    public void goodTest(String dName) throws Exception {
        deployer.deploy(dName);
        deployer.undeploy(dName);
    }

    public void badTest(String dName, String description) throws Exception {
        try {
            deployer.deploy(dName);
            fail("'" + dName + "' deployment shouldn't be deployed if " + description);
        } catch (Exception e) {
            // nothing
        } finally {
            deployer.undeploy(dName);
        }
    }

    @Test
    public void testValidationDisabled() throws Throwable {

        setArchiveValidation(false, false, false);
        goodTest("ok");
        goodTest("error");
        goodTest("warning");
    }

    @Test
    public void testValidationDisabled1() throws Throwable {

        setArchiveValidation(false, true, false);
        goodTest("ok");
        goodTest("error");
        goodTest("warning");
    }

    @Test
    public void testValidationDisabled2() throws Throwable {

        setArchiveValidation(false, false, true);
        goodTest("ok");
        goodTest("error");
        goodTest("warning");
    }

    @Test
    public void testValidationDisabled3() throws Throwable {

        setArchiveValidation(false, true, true);
        goodTest("ok");
        goodTest("error");
        goodTest("warning");
    }

    @Test
    public void testValidationEnabled() throws Throwable {

        setArchiveValidation(true, false, false);
        goodTest("ok");
        goodTest("error");
        goodTest("warning");
    }

    @Test
    public void testValidationOfErrorsEnabled() throws Throwable {

        setArchiveValidation(true, true, false);
        goodTest("ok");
        badTest("error", "fail on errors is enabled");
        goodTest("warning");
    }

    @Test
    public void testValidationOfErrorsAndWarningsEnabled() throws Throwable {

        setArchiveValidation(true, true, true);
        goodTest("ok");
        badTest("error", "fail on errors and warnings is enabled");
        badTest("warning", "fail on errors and warnings is enabled");
    }

    @Test
    public void testValidationOfWarningsEnabled() throws Throwable {

        setArchiveValidation(true, false, true);
        goodTest("ok");
        badTest("error", "fail on warnings is enabled");
        badTest("warning", "fail on warnings is enabled");
    }
}
