/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed;

/**
 * Base class for a test suite that uses a minimal domain config and primary host that has only elytron security
 */
public class ElytronOnlyPrimaryTestSuite extends MixedDomainTestSuite {


    protected static MixedDomainTestSupport getSupport(Class<?> testClass) {
        final Version.AsVersion version = getVersion(testClass);
        return getSupport(testClass, "primary-config/host-primary-elytron.xml",
                version.getDefaultSecondaryHostConfigFileName());
    }
}
