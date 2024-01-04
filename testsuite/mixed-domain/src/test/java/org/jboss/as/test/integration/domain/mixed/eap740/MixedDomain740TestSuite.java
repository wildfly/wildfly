/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import org.jboss.as.test.integration.domain.mixed.MixedDomainTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite for the standard mixed domain tests.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Suite.class)
@SuiteClasses(value= {SimpleMixedDomain740TestCase.class, MixedDomainDeployment740TestCase.class, PatchRemoteHost740TestCase.class})
@Version(AsVersion.EAP_7_4_0)
public class MixedDomain740TestSuite extends MixedDomainTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        MixedDomainTestSuite.getSupport(MixedDomain740TestSuite.class);
    }
}
