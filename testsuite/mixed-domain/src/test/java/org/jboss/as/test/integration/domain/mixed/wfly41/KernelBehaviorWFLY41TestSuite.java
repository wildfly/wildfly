/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly41;

import org.jboss.as.test.integration.domain.mixed.KernelBehaviorTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(value = { RBACConfigWFLY41TestCase.class, WildcardReadsWFLY41TestCase.class })
@Version(Version.AsVersion.WFLY_41_0_0)
public class KernelBehaviorWFLY41TestSuite extends KernelBehaviorTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        KernelBehaviorTestSuite.getSupport(KernelBehaviorWFLY41TestSuite.class);
    }
}
