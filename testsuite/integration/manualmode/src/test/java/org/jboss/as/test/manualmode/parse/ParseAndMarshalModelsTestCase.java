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

import java.io.File;
import java.io.FileNotFoundException;

import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.ModelParserUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the ability to parse the config files we ship or have shipped in the past, as well as the ability
 * to marshal them back to xml in a manner such that reparsing them produces a consistent in-memory configuration model.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ParseAndMarshalModelsTestCase {

    private static final File JBOSS_HOME = new File(".." + File.separatorChar + "jbossas-parse-marshal");

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
        standaloneXmlTest(getDocsExampleConfigFile("standalone-picketlink.xml"));
    }

    @Test
    public void testStandaloneXtsXml() throws Exception {
        standaloneXmlTest(getDocsExampleConfigFile("standalone-xts.xml"));
    }

    @Test
    public void testStandaloneJtsXml() throws Exception {
        standaloneXmlTest(getDocsExampleConfigFile("standalone-jts.xml"));
    }

    @Test
    public void testStandaloneGenericJMSXml() throws Exception {
        standaloneXmlTest(getDocsExampleConfigFile("standalone-genericjms.xml"));
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
    public void testEAP600StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-0-0-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP600StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-0-0-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP600StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-0-0-hornetq-colocated.xml"));
    }

    @Test
    public void testEAP600StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-0-0-jts.xml"));
    }

    @Test
    public void testEAP600StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-0-0-minimalistic.xml"));
    }

    @Test
    public void testEAP600StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-0-0-xts.xml"));
    }

    @Test
    public void testEAP610StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-1-0-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP610StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-1-0-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP610StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-1-0-hornetq-colocated.xml"));
    }

    @Test
    public void testEAP610StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-1-0-jts.xml"));
    }

    @Test
    public void testEAP610StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-1-0-minimalistic.xml"));
    }

    @Test
    public void testEAP610StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-1-0-xts.xml"));
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

    @Test
    public void testEAP630StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-3-0-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP630StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-3-0-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP630StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-3-0-hornetq-colocated.xml"));
    }

    @Test
    public void testEAP630StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-3-0-jts.xml"));
    }

    @Test
    public void testEAP630StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-3-0-minimalistic.xml"));
    }

    @Test
    public void testEAP630StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-3-0-xts.xml"));
    }

    @Test
    public void testEAP640StandaloneFullHaXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-4-0-full-ha.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP640StandaloneFullXml() throws Exception {
        ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-4-0-full.xml"));
        validateJsfSubsystem(model);
    }

    @Test
    public void testEAP640StandaloneHornetQCollocatedXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-4-0-hornetq-colocated.xml"));
    }

    @Test
    public void testEAP640StandaloneJtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-4-0-jts.xml"));
    }

    @Test
    public void testEAP640StandaloneMinimalisticXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-4-0-minimalistic.xml"));
    }

    @Test
    public void testEAP640StandaloneXtsXml() throws Exception {
        standaloneXmlTest(getLegacyConfigFile("standalone", "eap-6-4-0-xts.xml"));
    }

    private ModelNode standaloneXmlTest(File original) throws Exception {
        return ModelParserUtils.standaloneXmlTest(original, JBOSS_HOME);
    }

    @Test
    public void testHostXml() throws Exception {
        hostXmlTest(getOriginalHostXml("host.xml"));
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
    public void testEAP600HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "eap-6-0-0.xml"));
    }

    @Test
    public void testEAP610HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "eap-6-1-0.xml"));
    }

    @Test
    public void testEAP620HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "eap-6-2-0.xml"));
    }

    @Test
    public void testEAP630HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "eap-6-3-0.xml"));
    }

    @Test
    public void testEAP640HostXml() throws Exception {
        hostXmlTest(getLegacyConfigFile("host", "eap-6-4-0.xml"));
    }

    private void hostXmlTest(final File original) throws Exception {
        ModelParserUtils.hostXmlTest(original, JBOSS_HOME);
    }

    @Test
    public void testDomainXml() throws Exception {
        domainXmlTest(getOriginalDomainXml("domain.xml"));
    }

    @Test
    public void test713DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-1-3.xml"));
        validateJsfProfiles(model);
    }

    @Test
    public void test720DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "7-2-0.xml"));
        validateJsfProfiles(model);
    }

    @Test
    public void testEAP600DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "eap-6-0-0.xml"));
        validateJsfProfiles(model);
    }

    @Test
    public void testEAP610DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "eap-6-1-0.xml"));
        validateJsfProfiles(model);
    }

    @Test
    public void testEAP620DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "eap-6-2-0.xml"));
        validateJsfProfiles(model);
    }

    @Test
    public void testEAP630DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "eap-6-3-0.xml"));
        validateJsfProfiles(model);
    }

    @Test
    public void testEAP640DomainXml() throws Exception {
        ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "eap-6-4-0.xml"));
        validateJsfProfiles(model);
    }

    private ModelNode domainXmlTest(final File original) throws Exception {
        return ModelParserUtils.domainXmlTest(original, JBOSS_HOME);
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
                System.getProperty("jboss.dist"),
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
                System.getProperty("jboss.dist"),
                "docs/examples/configs" + File.separator + name
        );
    }

    private File getHostConfigDir() throws FileNotFoundException {
        //Get the standalone.xml from the build/src directory, since the one in the
        //built server could have changed during running of tests
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jboss.dist"),
                "domain/configuration"
        );
    }

    private File getDomainConfigDir() throws FileNotFoundException {
        return FileUtils.getFileOrCheckParentsIfNotFound(
                System.getProperty("jboss.dist"),
                "domain/configuration"
        );
    }
}
