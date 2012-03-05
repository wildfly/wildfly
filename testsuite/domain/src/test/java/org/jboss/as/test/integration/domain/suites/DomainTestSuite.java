/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.domain.suites;

import org.jboss.as.test.integration.domain.DomainTestSupport;
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
        CoreResourceManagementTestCase.class,
        ManagementReadsTestCase.class,
        DeploymentManagementTestCase.class,
        ServerManagementTestCase.class,
        ServerRestartRequiredTestCase.class,
        ManagementAccessTestCase.class,
        ManagementClientContentTestCase.class,
        ValidateOperationOperationTestCase.class
})
public class DomainTestSuite {

    private static final DomainTestSupport.Configuration CONFIGURATION;
    private static boolean initializedLocally = false;
    private static volatile DomainTestSupport support;

    static {
        CONFIGURATION = DomainTestSupport.Configuration.create("domain-configs/domain-standard.xml", "host-configs/host-master.xml", "host-configs/host-slave.xml");
    }

    static synchronized DomainTestSupport createSupport(final String testName) {
        if(support == null) {
            start(testName);
        }
        return support;
    }

    static synchronized void stopSupport() {
        if(! initializedLocally) {
            stop();
        }
    }

    private synchronized static void start(final String name) {
        try {
            final DomainTestSupport testSupport = new DomainTestSupport(name, CONFIGURATION);
            // Start!
            testSupport.start();
            support = testSupport;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized static void stop() {
        if(support != null) {
            support.stop();
            support = null;
        }
    }

    @BeforeClass
    public synchronized static void beforeClass() {
        initializedLocally = true;
        start(DomainTestSuite.class.getSimpleName());
    }

    @AfterClass
    public synchronized static void afterClass() {
        stop();
    }

}
