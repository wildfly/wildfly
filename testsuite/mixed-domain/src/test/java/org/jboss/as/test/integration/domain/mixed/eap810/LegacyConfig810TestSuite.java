/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap810;

import org.jboss.as.test.integration.domain.mixed.MixedDomainTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests of using EAP 8.1 domain.xml with a current DC and an 8.1 secondary.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(value= {
        LegacyConfig810TestCase.class,
        DomainHostExcludes810TestCase.class
})
@Version(Version.AsVersion.EAP_8_1_0)
public class LegacyConfig810TestSuite extends MixedDomainTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        MixedDomainTestSuite.getSupportForLegacyConfig(LegacyConfig810TestSuite.class, Version.AsVersion.EAP_8_1_0);
    }
}
