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

import java.nio.file.Path;

import org.junit.AfterClass;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MixedDomainTestSuite {
    public static enum Profile {
        FULL("full"), FULL_HA("full-ha"), DEFAULT("default");
        private final String profileName;
        private Profile(String profileName) {
            this.profileName = profileName;
        }

        public String getProfile() {
            return profileName;
        }
    }
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
            return getSupport(testClass, Profile.FULL_HA, false);
    }

protected static MixedDomainTestSupport getSupport(Class<?> testClass,  boolean withMasterServers) {
            return getSupport(testClass, Profile.FULL_HA, withMasterServers);
    }

    protected static MixedDomainTestSupport getSupport(Class<?> testClass, Profile profile, boolean withMasterServers) {
        if (support == null) {
            final String copiedDomainXml = MixedDomainTestSupport.copyDomainFile();
            return getSupport(testClass, copiedDomainXml, profile, true, false, withMasterServers);
        }
        return support;
    }
    /**
     * Call this from a @BeforeClass method
     *
     * @param testClass the test/suite class
     */
    protected static MixedDomainTestSupport getSupport(Class<?> testClass, String masterConfig, String slaveConfig, Profile profile, boolean withMasterServers) {
        return getSupport(testClass, masterConfig, slaveConfig, profile, true, false, withMasterServers);
    }

    protected static MixedDomainTestSupport getSupport(Class<?> testClass, String masterConfig, boolean adjustDomain, boolean legacyConfig, boolean withMasterServers) {
        return getSupport(testClass, masterConfig, null, Profile.FULL_HA, adjustDomain, legacyConfig, withMasterServers);
    }
    /**
     * Call this from a @BeforeClass method
     *
     * @param testClass the test/suite class
     */
    protected static MixedDomainTestSupport getSupport(Class<?> testClass, String masterConfig, String slaveConfig, Profile profile, boolean adjustDomain, boolean legacyConfig, boolean withMasterServers) {
        if (support == null) {
            final String copiedDomainXml = MixedDomainTestSupport.copyDomainFile();
            return getSupport(testClass, copiedDomainXml, masterConfig, slaveConfig, profile, adjustDomain, legacyConfig, withMasterServers);
        }
        return support;
    }

    protected static MixedDomainTestSupport getSupport(Class<?> testClass, String masterConfig, String slaveConfig) {
        if (support == null) {
            final String copiedDomainXml = MixedDomainTestSupport.copyDomainFile();
            return getSupport(testClass, copiedDomainXml, masterConfig, slaveConfig, Profile.FULL_HA, true, false, false);
        }
        return support;
    }

    /**
     * Call this from a @BeforeClass method
     *
     * @param testClass the test/suite class
     * @param version the version of the legacy slave.
     */
    protected static MixedDomainTestSupport getSupportForLegacyConfig(Class<?> testClass, Version.AsVersion version) {
        if (support == null) {
            final Path originalDomainXml = MixedDomainTestSupport.loadLegacyDomainXml(version);
            final String copiedDomainXml = MixedDomainTestSupport.copyDomainFile(originalDomainXml);
            return getSupport(testClass, copiedDomainXml, Profile.FULL_HA, true, true, false);
        }
        return support;
    }

    static MixedDomainTestSupport getSupport(Class<?> testClass, String domainConfig, Profile profile, boolean adjustDomain, boolean legacyConfig, boolean withMasterServers) {
        return getSupport(testClass, domainConfig, null, null, profile, adjustDomain, legacyConfig, withMasterServers);
    }

    static MixedDomainTestSupport getSupport(Class<?> testClass, String domainConfig, String masterConfig, String slaveConfig, Profile profile, boolean adjustDomain, boolean legacyConfig, boolean withMasterServers) {

        if (support == null) {
            final Version.AsVersion version = getVersion(testClass);
            final MixedDomainTestSupport testSupport;
            try {
                if (domainConfig != null) {
                    if(masterConfig != null || slaveConfig != null) {
                        testSupport = MixedDomainTestSupport.create(testClass.getSimpleName(), version, domainConfig, masterConfig, slaveConfig, profile.getProfile(),  adjustDomain, legacyConfig, withMasterServers);
                    } else {
                        testSupport = MixedDomainTestSupport.create(testClass.getSimpleName(), version, domainConfig, profile.getProfile(), adjustDomain, legacyConfig, withMasterServers);
                    }
                } else {
                    testSupport = MixedDomainTestSupport.create(testClass.getSimpleName(), version);
                }
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

    private static synchronized void stop() {
        if(support != null) {
            support.stop();
            support = null;
            version = null;
        }
    }

    @AfterClass
    public static synchronized void afterClass() {
        stop();
    }
}
