/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
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
        JcaCCMRuntimeOnlyProfileOpsTestCase.class,
        ModelPersistenceTestCase.class,
        ReadEnvironmentVariablesTestCase.class,
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
            support.stop();
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
