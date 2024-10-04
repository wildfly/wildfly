/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

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
        LegacyConfig740TestCase.class,
        DomainHostExcludes740TestCase.class
})
@Version(Version.AsVersion.EAP_7_4_0)
public class LegacyConfig740TestSuite extends MixedDomainTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        MixedDomainTestSuite.getSupportForLegacyConfig(LegacyConfig740TestSuite.class, Version.AsVersion.EAP_7_4_0);
    }
}
