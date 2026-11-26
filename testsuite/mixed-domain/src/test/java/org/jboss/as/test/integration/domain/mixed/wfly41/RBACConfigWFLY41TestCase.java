/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly41;

import org.jboss.as.test.integration.domain.mixed.RBACConfigTestCase;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

@Version(Version.AsVersion.WFLY_41_0_0)
public class RBACConfigWFLY41TestCase extends RBACConfigTestCase {

    @BeforeClass
    public static void beforeClass() {
        KernelBehaviorWFLY41TestSuite.initializeDomain();
    }
}
