/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import org.jboss.as.test.integration.domain.mixed.MixedDomainTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests secondary behavior in a mixed domain when the primary has booted with a legacy domain.xml.
 *
 * @author Brian Stansberry
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(value= {
        LegacyConfigWFLY31TestCase.class,
        DomainHostExcludesWFLY31TestCase.class
})
@Version(Version.AsVersion.WFLY_31_0_0)
public class LegacyConfigWFLY31TestSuite extends MixedDomainTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        MixedDomainTestSuite.getSupportForLegacyConfig(LegacyConfigWFLY31TestSuite.class, Version.AsVersion.WFLY_31_0_0);
    }
}
