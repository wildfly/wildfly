/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap810;

import org.jboss.as.test.integration.domain.mixed.RBACConfigTestCase;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

/**
 * EAP 8.1 variant of RBACConfigTestCase.
 */
@Version(Version.AsVersion.EAP_8_1_0)
public class RBACConfig810TestCase extends RBACConfigTestCase {

    @BeforeClass
    public static void beforeClass() {
        KernelBehavior810TestSuite.initializeDomain();
    }
}
