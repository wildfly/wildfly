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

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MixedDomainTestSuite {
    private static MixedDomainTestSupport support;
    private static Version.AsVersion version;


    static Version.AsVersion getVersion(Class<?> testClass) {
        final Version version = testClass.getAnnotation(Version.class);
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
     */
    protected static MixedDomainTestSupport getSupport(Class<?> testClass) {
        final Version.AsVersion version = getVersion(testClass);
        if (support == null) {
            final MixedDomainTestSupport testSupport;
            try {
                testSupport = MixedDomainTestSupport.create(testClass.getSimpleName(), version);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                //Start the the domain with adjustments to domain.xml
                testSupport.start();
                support = testSupport;
            } catch (Exception e) {
                testSupport.stop();
                throw new RuntimeException(e);
            }
        }
        return support;
    }

    protected Version.AsVersion getVersion() {
        return version;
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
