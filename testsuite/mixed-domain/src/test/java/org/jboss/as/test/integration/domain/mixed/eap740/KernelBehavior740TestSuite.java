/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

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
@Suite.SuiteClasses(value= {RBACConfig740TestCase.class, WildcardReads740TestCase.class})
@Version(Version.AsVersion.EAP_7_4_0)
public class KernelBehavior740TestSuite extends KernelBehaviorTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        KernelBehaviorTestSuite.getSupport(KernelBehavior740TestSuite.class);
    }
}
