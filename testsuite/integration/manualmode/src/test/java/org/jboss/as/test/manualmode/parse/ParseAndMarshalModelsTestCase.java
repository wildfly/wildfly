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
 * Tests the ability to parse the config files we ship or have shipped in the past, as well as the ability to marshal
 * them back to xml in a manner such that reparsing them produces a consistent in-memory configuration model.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ParseAndMarshalModelsTestCase {

    private static final String[] EAP_VERSIONS = {"6-0-0", "6-1-0", "6-2-0", "6-3-0", "6-4-0"};
    private static final String[] AS_VERSIONS = {"7-1-3", "7-2-0"};

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
    public void testJBossASStandaloneXml() throws Exception {
        for (String version : AS_VERSIONS) {
            ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", version + ".xml"));
            validateJsfSubsystem(model, version);
        }
    }

    @Test
    public void testJBossASStandaloneFullHaXml() throws Exception {
        for (String version : AS_VERSIONS) {
            ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", version + "-full-ha.xml"));
            validateJsfSubsystem(model, version);
        }
    }

    @Test
    public void testJBossASStandaloneFullXml() throws Exception {
        for (String version : AS_VERSIONS) {
            ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", version + "-full.xml"));
            validateJsfSubsystem(model, version);
        }
    }

    @Test
    public void testJBossASStandaloneHornetQCollocatedXml() throws Exception {
        for (String version : AS_VERSIONS) {
            standaloneXmlTest(getLegacyConfigFile("standalone", version + "-hornetq-colocated.xml"));
        }
    }

    @Test
    public void testJBossASStandaloneJtsXml() throws Exception {
        for (String version : AS_VERSIONS) {
            standaloneXmlTest(getLegacyConfigFile("standalone", version + "-jts.xml"));
        }
    }

    @Test
    public void testJBossASStandaloneMinimalisticXml() throws Exception {
        for (String version : AS_VERSIONS) {
            standaloneXmlTest(getLegacyConfigFile("standalone", version + "-minimalistic.xml"));
        }
    }

    @Test
    public void testJBossASStandaloneXtsXml() throws Exception {
        for (String version : AS_VERSIONS) {
            standaloneXmlTest(getLegacyConfigFile("standalone", version + "-xts.xml"));
        }
    }

    @Test
    public void testEAPStandaloneFullHaXml() throws Exception {
        for (String version : EAP_VERSIONS) {
            ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-" + version + "-full-ha.xml"));
            validateWebSubsystem(model, version);
            validateJsfSubsystem(model, version);
            validateCmpSubsystem(model, version);
            validateMessagingSubsystem(model, version);
        }
    }

    @Test
    public void testEAPStandaloneFullXml() throws Exception {
        for (String version : EAP_VERSIONS) {
            ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-" + version + "-full.xml"));
            validateWebSubsystem(model, version);
            validateJsfSubsystem(model, version);
            validateCmpSubsystem(model, version);
            validateMessagingSubsystem(model, version);
        }
    }

    @Test
    public void testEAPStandaloneXml() throws Exception {
        for (String version : EAP_VERSIONS) {
            ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-" + version + ".xml"));
            validateWebSubsystem(model, version);
            validateJsfSubsystem(model, version);
        }
    }

    @Test
    public void testEAPStandaloneHornetQCollocatedXml() throws Exception {
        for (String version : EAP_VERSIONS) {
            ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-" + version + "-hornetq-colocated.xml"));
            validateWebSubsystem(model, version);
            validateJsfSubsystem(model, version);
            validateMessagingSubsystem(model, version);
            validateThreadsSubsystem(model, version);
            validateJacordSubsystem(model, version);
        }
    }

    @Test
    public void testEAPStandaloneJtsXml() throws Exception {
        for (String version : EAP_VERSIONS) {
            ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-" + version + "-jts.xml"));
            validateWebSubsystem(model, version);
            validateJsfSubsystem(model, version);
            validateThreadsSubsystem(model, version);
            validateJacordSubsystem(model, version);
        }
    }

    @Test
    public void testEAPStandaloneMinimalisticXml() throws Exception {
        for (String version : EAP_VERSIONS) {
            standaloneXmlTest(getLegacyConfigFile("standalone", "eap-" + version + "-minimalistic.xml"));
        }
    }

    @Test
    public void testEAPStandaloneXtsXml() throws Exception {
        for (String version : EAP_VERSIONS) {
            ModelNode model = standaloneXmlTest(getLegacyConfigFile("standalone", "eap-" + version + "-xts.xml"));
            validateCmpSubsystem(model, version);
            validateWebSubsystem(model, version);
            validateJsfSubsystem(model, version);
            validateThreadsSubsystem(model, version);
            validateJacordSubsystem(model, version);
            validateXtsSubsystem(model, version);
        }
    }

    private ModelNode standaloneXmlTest(File original) throws Exception {
        return ModelParserUtils.standaloneXmlTest(original, JBOSS_HOME);
    }

    @Test
    public void testHostXml() throws Exception {
        hostXmlTest(getOriginalHostXml("host.xml"));
    }

    @Test
    public void testJBossASHostXml() throws Exception {
        for (String version : AS_VERSIONS) {
            hostXmlTest(getLegacyConfigFile("host", version + ".xml"));
        }
    }

    @Test
    public void testEAPHostXml() throws Exception {
        for (String version : EAP_VERSIONS) {
            hostXmlTest(getLegacyConfigFile("host", "eap-" + version + ".xml"));
        }
    }

    private void hostXmlTest(final File original) throws Exception {
        ModelParserUtils.hostXmlTest(original, JBOSS_HOME);
    }

    @Test
    public void testDomainXml() throws Exception {
        domainXmlTest(getOriginalDomainXml("domain.xml"));
    }

    @Test
    public void testJBossASDomainXml() throws Exception {
        for (String version : AS_VERSIONS) {
            ModelNode model = domainXmlTest(getLegacyConfigFile("domain", version + ".xml"));
            validateProfiles(model, version);
        }
    }

    @Test
    public void testEAPDomainXml() throws Exception {
        for (String version : EAP_VERSIONS) {
            ModelNode model = domainXmlTest(getLegacyConfigFile("domain", "eap-" + version + ".xml"));
            validateProfiles(model, version);
        }
    }

    private ModelNode domainXmlTest(final File original) throws Exception {
        return ModelParserUtils.domainXmlTest(original, JBOSS_HOME);
    }

    private static void validateProfiles(ModelNode model, String version) {
        Assert.assertTrue(model.hasDefined(PROFILE));
        for (Property prop : model.get(PROFILE).asPropertyList()) {
            validateWebSubsystem(prop.getValue(), version);
            validateJsfSubsystem(prop.getValue(), version);
            validateThreadsSubsystem(prop.getValue(), version);
        }
    }

    private static void validateWebSubsystem(ModelNode model, String version) {
        validateSubsystem(model, "web", version);
        Assert.assertTrue(model.hasDefined(SUBSYSTEM, "web", "connector", "http"));
    }

    private static void validateJsfSubsystem(ModelNode model, String version) {
        validateSubsystem(model, "jsf", version); //we cannot check for it as web subsystem is not present to add jsf one
    }

    private static void validateCmpSubsystem(ModelNode model, String version) {
        validateSubsystem(model, "cmp", version);
    }

    private static void validateMessagingSubsystem(ModelNode model, String version) {
        validateSubsystem(model, "messaging", version);
        Assert.assertTrue(model.hasDefined(SUBSYSTEM, "messaging", "hornetq-server", "default"));
    }

    private static void validateThreadsSubsystem(ModelNode model, String version) {
        validateSubsystem(model, "threads", version);
    }

    private static void validateJacordSubsystem(ModelNode model, String version) {
        validateSubsystem(model, "jacorb", version);
    }

    private static void validateXtsSubsystem(ModelNode model, String version) {
        validateSubsystem(model, "xts", version);
        Assert.assertTrue(model.hasDefined(SUBSYSTEM, "xts", "host"));
    }

    private static void validateSubsystem(ModelNode model, String subsystem, String version) {
        Assert.assertTrue(model.hasDefined(SUBSYSTEM));
        Assert.assertTrue("Missing " + subsystem + " subsystem for " + version, model.get(SUBSYSTEM).hasDefined(subsystem));
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
