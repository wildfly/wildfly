/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap800;

import org.jboss.as.test.integration.domain.mixed.MixedDomainTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests of using EAP 7.4 domain.xml with a current DC and a 7.4 secondary.
 *
 * @author Brian Stansberry
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(value= {
        LegacyConfig800TestCase.class,
        DomainHostExcludes800TestCase.class
})
@Version(Version.AsVersion.EAP_8_0_0)
public class LegacyConfig800TestSuite extends MixedDomainTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        MixedDomainTestSuite.getSupportForLegacyConfig(LegacyConfig800TestSuite.class, Version.AsVersion.EAP_8_0_0);
    }
}
