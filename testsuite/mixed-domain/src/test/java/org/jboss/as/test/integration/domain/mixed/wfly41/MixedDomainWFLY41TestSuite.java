/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly41;

import org.jboss.as.test.integration.domain.mixed.MixedDomainTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(value = { SimpleMixedDomainWFLY41TestCase.class, MixedDomainDeploymentWFLY41TestCase.class })
@Version(Version.AsVersion.WFLY_41_0_0)
public class MixedDomainWFLY41TestSuite extends MixedDomainTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        MixedDomainTestSuite.getSupport(MixedDomainWFLY41TestSuite.class);
    }
}
