/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.suites;

import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Simple {@code Suite} test wrapper to start the domain only once for multiple
 * test cases using the same domain configuration.
 *
 * @author Emanuel Muckenhuber
 */
@RunWith(Suite.class)
@Suite.SuiteClasses ({
        DatasourceTestCase.class,
        DeploymentManagementTestCase.class,
        DeploymentOverlayTestCase.class,
        GlobalDirectoryDomainTestCase.class,
        JcaCCMRuntimeOnlyProfileOpsTestCase.class,
        ModelPersistenceTestCase.class,
        ReadEnvironmentVariablesTestCase.class
})
public class DomainTestSuite {

    private static boolean initializedLocally = false;
    private static volatile DomainTestSupport support;

    // This can only be called from tests as part of this suite
    public static synchronized DomainTestSupport createSupport(final String testName) {
        if(support == null) {
            start(testName);
        }
        return support;
    }

    // This can only be called from tests as part of this suite
    public static synchronized void stopSupport() {
        if(! initializedLocally) {
            stop();
        }
    }

    private static synchronized void start(final String name) {
        try {
            support = DomainTestSupport.createAndStartDefaultSupport(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static synchronized void stop() {
        if(support != null) {
            support.close();
            support = null;
        }
    }

    @BeforeClass
    public static synchronized void beforeClass() {
        initializedLocally = true;
        start(DomainTestSuite.class.getSimpleName());
    }

    @AfterClass
    public static synchronized void afterClass() {
        stop();
    }

}
