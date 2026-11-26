/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly41;

import static org.jboss.as.test.integration.domain.mixed.Version.AsVersion.WFLY_41_0_0;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.as.test.integration.domain.mixed.DomainHostExcludesTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.junit.BeforeClass;

@Version(WFLY_41_0_0)
public class DomainHostExcludesWFLY41TestCase extends DomainHostExcludesTest {

    @BeforeClass
    public static void beforeClass()
            throws InterruptedException, TimeoutException, MgmtOperationException, IOException {
        LegacyConfigWFLY41TestSuite.initializeDomain();
        setup(DomainHostExcludesWFLY41TestCase.class, WFLY_41_0_0.getHostExclude(), WFLY_41_0_0.getModelVersion());
    }
}
