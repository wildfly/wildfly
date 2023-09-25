/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import org.jboss.as.test.integration.domain.mixed.RBACConfigTestCase;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

/**
 * EAP 7.4 variant of RBACConfigTestCase.
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.EAP_7_4_0)
public class RBACConfig740TestCase extends RBACConfigTestCase {

    @BeforeClass
    public static void beforeClass() {
        KernelBehavior740TestSuite.initializeDomain();
    }
}
