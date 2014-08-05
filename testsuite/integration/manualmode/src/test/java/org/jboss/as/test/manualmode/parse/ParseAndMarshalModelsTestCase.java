/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.manualmode.parse;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.FileUtils;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the ability to parse the config files we ship or have shipped in the past, as well as the ability
 * to marshal them back to xml in a manner such that reparsing them produces a consistent in-memory configuration model.
 * <p/>
 * <b>Note:</b>The standard {@code build/src/main/resources/standalone/configuration} and
 * {@code build/src/main/resources/domain/configuration} files are tested in the smoke integration module ParseAndMarshalModelsTestCase.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @see org.jboss.as.test.parsing.ModelParserUtils
 */
@RunWith(Arquillian.class)
@Ignore("WFLY-3638: use org.jboss.as.test.shared.ModelParserUtils whith new version of core.")
public class ParseAndMarshalModelsTestCase {

    @Deployment(name = "test", managed = false, testable = true)
    public static Archive<?> getDeployment() {

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bogus.jar");
        archive.addPackage(ParseAndMarshalModelsTestCase.class.getPackage());
        archive.addClass(FileUtils.class);
        //WFLY-3638: archive.addClass(ModelParserUtils.class);
        archive.add(new Asset() {
            public InputStream openStream() {
                return new ByteArrayInputStream("Dependencies: org.jboss.staxmapper,org.jboss.as.controller,org.jboss.as.deployment-repository,org.jboss.as.server,org.jboss.as.host-controller,org.jboss.as.domain-management,org.jboss.as.security\n\n".getBytes());
            }
        }, "META-INF/MANIFEST.MF");
        return archive;
    }

    @Test
    @InSequence(-1)
    @RunAsClient
    public void start(@ArquillianResource ContainerController cc, @ArquillianResource Deployer deployer) {
        cc.start("default-jbossas");
        deployer.deploy("test");
    }

    @Test
    @InSequence(1)
    @RunAsClient
    public void stop(@ArquillianResource ContainerController cc, @ArquillianResource Deployer deployer) {
        deployer.undeploy("test");
        cc.stop("default-jbossas");
    }

    @Test
    public void testStandaloneXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone.xml"));
    }

    @Test
    public void testStandaloneHAXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone-ha.xml"));
    }

    @Test
    public void testStandaloneFullXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone-full.xml"));
    }

    @Test
    public void testStandaloneFullHAXml() throws Exception {
        standaloneXmlTest(getOriginalStandaloneXml("standalone-full-ha.xml"));
    }

    @Test
    public void testStandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getDocsExampleConfigFile("standalone-minimalistic.xml"));
    }

    @Test
    public void testStandalonePicketLinkXml() throws Exception {
        standaloneXmlTest(getGeneratedExampleConfigFile("standalone-picketlink.xml"));
    }

    @Test
    public void testStandaloneXtsXml() throws Exception {
        standaloneXmlTest(getGeneratedExampleConfigFile("standalone-xts.xml"));
    }

    @Test
    public void testStandaloneHornetqColocatedXml() throws Exception {
        standaloneXmlTest(getGeneratedExampleConfigFile("standalone-hornetq-colocated.xml"));
    }

    @Test
    public void testStandaloneJtsXml() throws Exception {
        standaloneXmlTest(getGeneratedExampleConfigFile("standalone-jts.xml"));
    }

    @Test
    public void testStandaloneGenericJMSXml() throws Exception {
        standaloneXmlTest(getGeneratedExampleConfigFile("standalone-genericjms.xml"));
    }

    @Test
    public void test710StandaloneXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0.xml"));
    }

    @Test
    public void test710StandaloneFullHaXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-full-ha.xml"));
    }

    @Test
    public void test710StandaloneFullXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-full.xml"));
    }

    @Test
    public void test710StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-hornetq-colocated.xml"));
    }

    @Test
    public void test710StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-jts.xml"));
    }

    @Test
    public void test710StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-minimalistic.xml"));
    }

    @Test
    public void test710StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-0-xts.xml"));
    }

    @Test
    public void test711StandaloneXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1.xml"));
    }

    @Test
    public void test711StandaloneFullHaXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-full-ha.xml"));
    }

    @Test
    public void test711StandaloneFullXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-full.xml"));
    }

    @Test
    public void test711StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-hornetq-colocated.xml"));
    }

    @Test
    public void test711StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-jts.xml"));
    }

    @Test
    public void test711StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-minimalistic.xml"));
    }

    @Test
    public void test711StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-1-xts.xml"));
    }

    @Test
    public void test712StandaloneXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test712StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test712StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test712StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-hornetq-colocated.xml"));
    }

    @Test
    public void test712StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-jts.xml"));
    }

    @Test
    public void test712StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-minimalistic.xml"));
    }

    @Test
    public void test712StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-2-xts.xml"));
    }

    @Test
    public void test713StandaloneXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test713StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test713StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test713StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-hornetq-colocated.xml"));
    }

    @Test
    public void test713StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-jts.xml"));
    }

    @Test
    public void test713StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-minimalistic.xml"));
    }

    @Test
    public void test713StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-1-3-xts.xml"));
    }

    @Test
    public void test720StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test720StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void test720StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-hornetq-colocated.xml"));
    }

    @Test
    public void test720StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-jts.xml"));
    }

    @Test
    public void test720StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-minimalistic.xml"));
    }

    @Test
    public void test720StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "7-2-0-xts.xml"));
    }

    @Test
    public void testEAP620StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP620StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP620StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-hornetq-colocated.xml"));
    }

    @Test
    public void testEAP620StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-jts.xml"));
    }

    @Test
    public void testEAP620StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-minimalistic.xml"));
    }

    @Test
    public void testEAP620StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-2-0-xts.xml"));
    }

    private ModelNode standaloneXmlTest(File original) throws Exception {
        File file = new File("target/standalone-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        throw new UnsupportedOperationException("Use \"return ModelParserUtils.standaloneXmlTest(original, file);\" when it is ready");
    }

    @Test
    public void testHostXml() throws Exception {
        hostXmlTest(getOriginalHostXml("host.xml"));
    }

    @Test
    public void test710HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-1-0.xml"));
    }

    @Test
    public void test711HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-1-1.xml"));
    }

    @Test
    public void test712HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-1-2.xml"));
    }

    @Test
    public void test713HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-1-3.xml"));
    }

    @Test
    public void test720HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "7-2-0.xml"));
    }

    @Test
    public void testEAP620HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "eap-6-2-0.xml"));
    }

    private void hostXmlTest(final File original) throws Exception {
        File file = new File("target/host-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        throw new UnsupportedOperationException("Use \"ModelParserUtils.hostXmlTest(original, file);\" when it is ready");
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void testDomainXml() throws Exception {
        domainXmlTest(getOriginalDomainXml("domain.xml"));
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test710DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-1-0.xml"));
        validateJsfProfiles(model);
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test711DomainXml() throws Exception {
        domainXmlTest(getLegacyConfigFile("domain", "7-1-1.xml"));
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test712DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-1-2.xml"));
        validateJsfProfiles(model);
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test713DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-1-3.xml"));
        validateJsfProfiles(model);
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void test720DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-2-0.xml"));
        validateJsfProfiles(model);
    }

    @Test
    @TargetsContainer("class-jbossas")
    public void testEAP620DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "eap-6-2-0.xml"));
        validateJsfProfiles(model);
    }

    private ModelNode domainXmlTest(final File original) throws Exception {
        File file = new File("target/domain-copy.xml");
        if (file.exists()) {
            file.delete();
        }
        throw new UnsupportedOperationException("Use \"return ModelParserUtils.domainXmlTest(original, file);\" when it is ready");
    }

    private static void validateJsfProfiles(ModelNode model) {
        Assert.assertTrue(model.hasDefined(PROFILE));
        for (Property prop : model.get(PROFILE).asPropertyList()) {
            validateJsfSubsystem(prop.getValue());
        }
    }

    private static void validateJsfSubsystem(ModelNode model) {
        Assert.assertTrue(model.hasDefined(SUBSYSTEM));
        //Assert.assertTrue(model.get(SUBSYSTEM).hasDefined("jsf")); //we cannot check for it as web subsystem is not present to add jsf one
    }
    //  Get-config methods

    private File getOriginalStandaloneXml(String profile) throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jboss.inst", "../../.."),
                "standalone/configuration/" + profile
        );
    }

    private File getOriginalHostXml(final String profile) throws FileNotFoundException {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        File f = getHostConfigDir();
        f = new File(f, profile);
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        return f;
    }

    private File getOriginalDomainXml(final String profile) throws FileNotFoundException {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        File f = getDomainConfigDir();
        f = new File(f, profile);
        Assert.assertTrue("Not found: " + f.getPath(), f.exists());
        return f;
    }

    private File getLegacyConfigFile(String type, String profile) throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jbossas.ts.submodule.dir"),
                "src/test/resources/legacy-configs/" + type + File.separator + profile
        );
    }

    private File getDocsExampleConfigFile(String name) throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jboss.inst", "../../.."),
                "docs/examples/configs" + File.separator + name
        );
    }

    private File getGeneratedExampleConfigFile(String name) throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jboss.inst", "../../.."),
                "docs/examples/configs" + File.separator + name
        );
    }

    private File getHostConfigDir() throws FileNotFoundException {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jboss.inst", "../../.."),
                "domain/configuration"
        );
    }

    private File getDomainConfigDir() throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jboss.inst", "../../.."),
                "domain/configuration"
        );
    }
}
