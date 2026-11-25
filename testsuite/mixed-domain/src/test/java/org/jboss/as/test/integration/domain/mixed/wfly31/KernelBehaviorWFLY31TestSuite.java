/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import org.jboss.as.test.integration.domain.mixed.KernelBehaviorTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author Brian Stansberry
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(value= {RBACConfigWFLY31TestCase.class, WildcardReadsWFLY31TestCase.class})
@Version(Version.AsVersion.WFLY_31_0_0)
public class KernelBehaviorWFLY31TestSuite extends KernelBehaviorTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        KernelBehaviorTestSuite.getSupport(KernelBehaviorWFLY31TestSuite.class);
    }
}
