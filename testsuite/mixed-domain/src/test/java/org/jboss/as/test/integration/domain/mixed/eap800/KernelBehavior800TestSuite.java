/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap800;

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
@Suite.SuiteClasses(value= {RBACConfig800TestCase.class, WildcardReads800TestCase.class})
@Version(Version.AsVersion.EAP_8_0_0)
public class KernelBehavior800TestSuite extends KernelBehaviorTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        KernelBehaviorTestSuite.getSupport(KernelBehavior800TestSuite.class);
    }
}
