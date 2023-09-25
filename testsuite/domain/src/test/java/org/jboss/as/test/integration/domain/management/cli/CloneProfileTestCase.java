/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.domain.management.cli;

import org.apache.commons.io.FileUtils;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Test for providing profile cloning ability (runtime (CLI)) to create
 * new profiles based on existing JBoss profiles.
 *
 * Test clone "default" profile to "clone-profile-test-case-default" profile.
 *
 * https://issues.jboss.org/browse/WFLY-4838
 *
 * @author Marek Kopecky <mkopecky@redhat.com>
 */
public class CloneProfileTestCase extends AbstractCliTestBase {

    private static Logger log = Logger.getLogger(CloneProfileTestCase.class);

    private static final String ORIGINAL_PROFILE = "default";

    private static final String NEW_PROFILE = "clone-profile-test-case-default";

    /**
     * Domain configuration
     */
    private static File domainCfg;

    @BeforeClass
    public static void before() throws Exception {
        DomainTestSupport domainSupport = CLITestSuite.createSupport(CloneProfileTestCase.class.getSimpleName());
        AbstractCliTestBase.initCLI(DomainTestSupport.primaryAddress);

        File primaryDir = new File(domainSupport.getDomainPrimaryConfiguration().getDomainDirectory());
        domainCfg = new File(primaryDir, "configuration"
                + File.separator + "testing-domain-standard.xml");
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
        CLITestSuite.stopSupport();
    }

    /**
     * Sends command line to CLI, validate and return output.
     *
     * @param line command line
     * @return CLI output
     */
    private String cliRequest(String line, boolean successRequired) {
        log.trace(line);
        cli.sendLine(line);
        String output = cli.readOutput();
        if (successRequired) {
            assertTrue("CLI command \"" + line + " doesn't contain \"success\"", output.contains("success"));
        }
        return output;
    }

    @Test
    public void testProfile() throws IOException {
        // get domain configuration
        String domainCfgContent = FileUtils.readFileToString(domainCfg);
        assertTrue("Domain configuration is not initialized correctly.", domainCfgContent.indexOf(NEW_PROFILE) == -1);

        // clone profile
        cliRequest("/profile=" + ORIGINAL_PROFILE + ":clone(to-profile=" + NEW_PROFILE + ")", true);

        // get and check submodules
        String originSubmodules = cliRequest("ls /profile=" + ORIGINAL_PROFILE + "/subsystem", false);
        String newSubmodules = cliRequest("ls /profile=" + NEW_PROFILE + "/subsystem", false);
        assertEquals("New profile has different submodules than origin profile.", originSubmodules, newSubmodules);

        // check domain configuration
        domainCfgContent = FileUtils.readFileToString(domainCfg);
        assertTrue("Domain configuration doesn't contain " + NEW_PROFILE + "profile.", domainCfgContent.indexOf(NEW_PROFILE) != -1);
    }
}
