/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap810;

import org.jboss.as.test.integration.domain.mixed.MixedDomainTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite for the mixed domain deployment overlay tests.
 */
@RunWith(Suite.class)
@SuiteClasses(value= {MixedDomainDeploymentOverlay810TestCase.class})
@Version(AsVersion.EAP_8_1_0)
public class MixedDomainOverlay810TestSuite extends MixedDomainTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        MixedDomainTestSuite.getSupport(MixedDomainOverlay810TestSuite.class, "primary-config/host.xml", "secondary-config/host-secondary-overlay.xml", Profile.DEFAULT, true, false, true);
    }
}
