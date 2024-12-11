/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import static org.jboss.as.test.integration.domain.mixed.Version.AsVersion.WFLY_31_0_0;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.as.test.integration.domain.mixed.DomainHostExcludesTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.junit.BeforeClass;

/**
 * Tests of the ability of a DC to exclude resources from visibility to a legacy secondary.
 *
 * @author Brian Stansberry
 */
@Version(WFLY_31_0_0)
public class DomainHostExcludesWFLY31TestCase extends DomainHostExcludesTest {

    @BeforeClass
    public static void beforeClass() throws InterruptedException, TimeoutException, MgmtOperationException, IOException {
        LegacyConfigWFLY31TestSuite.initializeDomain();
        setup(DomainHostExcludesWFLY31TestCase.class, WFLY_31_0_0.getHostExclude(), WFLY_31_0_0.getModelVersion());
    }
}
