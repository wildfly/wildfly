/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.mixed.util;

import org.junit.AfterClass;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractMixedDomainTest {
    private static volatile MixedDomainTestSupport support;

    protected MixedDomainTestSupport getSupport() {
        return support;
    }

    private synchronized static void start(final String name, final String version) {
        try {
            final MixedDomainTestSupport testSupport = MixedDomainTestSupport.create(name, version);
            // Start!
            testSupport.start();
            support = testSupport;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private synchronized static void stop() {
        if(support != null) {
            support.stop();
            support = null;
        }
    }

    /**
     * Call this from a @BeforeClass method
     *
     * @param testClass the test/suite class
     * @param version the version to test
     */
    public synchronized static void beforeClass(Class<?> testClass, String version) {
        start(testClass.getSimpleName(), version);
    }

    @AfterClass
    public synchronized static void afterClass() {
        stop();
    }

}
