/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed;

/**
 * Base class for a test suite that uses a minimal domain config in order
 * to not have to deal with subsystem configuration compatibility issues
 * across releases in tests that are focused on the behavior of the kernel.
 *
 * @author Brian Stansberry
 */
public class KernelBehaviorTestSuite extends MixedDomainTestSuite {

    /**
     * Call this from a @BeforeClass method
     *
     * @param testClass the test/suite class
     * @return
     */
    protected static MixedDomainTestSupport getSupport(Class<?> testClass) {
        return getSupport(testClass, "primary-config/domain-minimal.xml", null, null, Profile.DEFAULT, false, false, false);
    }
}
