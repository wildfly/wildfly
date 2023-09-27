/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed;

import org.junit.AfterClass;

public class ElytronOnlyPrimarySmokeTestCase extends SimpleMixedDomainTest {

    @Override
    public void init() throws Exception {
        support = ElytronOnlyPrimaryTestSuite.getSupport(this.getClass());
        version = ElytronOnlyPrimaryTestSuite.getVersion(this.getClass());
    }

    @AfterClass
    public static synchronized void afterClass() {
        ElytronOnlyPrimaryTestSuite.afterClass();
    }
}
