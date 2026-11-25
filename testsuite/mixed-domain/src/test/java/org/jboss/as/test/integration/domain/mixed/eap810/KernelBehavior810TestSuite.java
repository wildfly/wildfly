/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap810;

import org.jboss.as.test.integration.domain.mixed.KernelBehaviorTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * EAP 8.1 variant of the kernel behavior test suite.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(value= {RBACConfig810TestCase.class, WildcardReads810TestCase.class})
@Version(Version.AsVersion.EAP_8_1_0)
public class KernelBehavior810TestSuite extends KernelBehaviorTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        KernelBehaviorTestSuite.getSupport(KernelBehavior810TestSuite.class);
    }
}
