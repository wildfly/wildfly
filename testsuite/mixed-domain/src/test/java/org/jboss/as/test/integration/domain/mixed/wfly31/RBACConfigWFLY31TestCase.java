/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import org.jboss.as.test.integration.domain.mixed.RBACConfigTestCase;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.eap800.KernelBehavior800TestSuite;
import org.junit.BeforeClass;

/**
 * WildFly 31 variant of RBACConfigTestCase.
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.WFLY_31_0_0)
public class RBACConfigWFLY31TestCase extends RBACConfigTestCase {

    @BeforeClass
    public static void beforeClass() {
        KernelBehavior800TestSuite.initializeDomain();
    }
}
