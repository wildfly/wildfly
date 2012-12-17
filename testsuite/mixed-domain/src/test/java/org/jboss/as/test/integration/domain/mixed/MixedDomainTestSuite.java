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
package org.jboss.as.test.integration.domain.mixed;

import static junit.framework.Assert.assertEquals;

import org.jboss.as.test.integration.domain.mixed.util.MixedDomainTestSupport;
import org.junit.AfterClass;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MixedDomainTestSuite {
    private static MixedDomainTestSupport support;
    private static Version.AsVersion version;


    static Version.AsVersion getVersion(Class<?> testClass) {
        Version version = testClass.getAnnotation(Version.class);
        if (version == null) {
            throw new IllegalArgumentException("No @Version");
        }
        if (MixedDomainTestSuite.version != null) {
            assertEquals(MixedDomainTestSuite.version, version.value());
        }
        MixedDomainTestSuite.version = version.value();
        return version.value();
    }

    /**
     * Call this from a @BeforeClass method
     *
     * @param testClass the test/suite class
     * @param version the version to test
     */
    static MixedDomainTestSupport getSupport(Class<?> testClass) {
        Version.AsVersion version = getVersion(testClass);
        if (support == null) {
            try {
                final MixedDomainTestSupport testSupport = MixedDomainTestSupport.create(testClass.getSimpleName(), version.getVersion());
                // Start!
                testSupport.start();
                support = testSupport;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return support;
    }

    protected Version.AsVersion getVersion() {
        return version;
    }

    protected MixedDomainTestSupport getSupport() {
        return support;
    }


    private synchronized static void stop() {
        if(support != null) {
            support.stop();
            support = null;
            version = null;
        }
    }

    @AfterClass
    public synchronized static void afterClass() {
        stop();
    }
}
