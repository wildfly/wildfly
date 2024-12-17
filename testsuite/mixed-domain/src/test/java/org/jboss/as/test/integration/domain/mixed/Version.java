/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.domain.mixed;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.version.Stability;
import org.junit.Assume;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.TYPE)
public @interface Version {

    AsVersion value();

    String WILDFLY = "wildfly-";
    String EAP = "jboss-eap-";

    enum AsVersion {
        EAP_7_4_0(EAP, 7, 4, 0, 11, 8, "EAP7.4", ModelVersion.create(16, 0), Stability.DEFAULT, true),
        EAP_8_0_0(EAP, 8, 0, 0, 17, 11, "EAP8.0", ModelVersion.create(22, 0), Stability.DEFAULT, false),
        WFLY_31_0_0(WILDFLY, 31, 0, 0, 17, 11, "WildFly31.0", ModelVersion.create(24, 0), Stability.COMMUNITY, false),
        ;



        final String basename;
        private final int major;
        private final int minor;
        private final int micro;
        private final int maxVM;
        private final int minVM;
        final String version;
        final String hostExclude;
        final ModelVersion modelVersion;
        private final Stability stability;
        private final boolean useManagementRealms;

        /**
         * Metadata related to the server version we are using as secondary
         *
         * @param basename            Base name of the server, used to locate the zip file that contains the secondary under test.
         * @param major               Major release number
         * @param minor               Minor release number
         * @param micro               Micro release number
         * @param maxVM               The maximum Java version under which a legacy host can properly execute tests
         * @param minVM               The minimum Java version under which a legacy host can properly execute tests
         * @param hostExclude         The host-exclude name that represents this secondary
         * @param modelVersion        The Kernel version of this secondary
         * @param stability           The stability level of this secondary host controller
         * @param useManagementRealms Whether the secondary host controller uses management realms based security
         */
        AsVersion(String basename, int major, int minor, int micro, int maxVM, int minVM, String hostExclude, ModelVersion modelVersion, Stability stability, boolean useManagementRealms) {
            this.basename = basename;
            this.major = major;
            this.minor = minor;
            this.micro = micro;
            this.version = major + "." + minor + "." + micro;
            this.maxVM = maxVM;
            this.minVM = minVM;
            this.hostExclude = hostExclude;
            this.modelVersion = modelVersion;
            this.stability = stability;
            this.useManagementRealms = useManagementRealms;
        }

        public String getBaseName() {
            return basename;
        }

        public String getVersion() {
            return version;
        }

        public String getFullVersionName() {
            return basename + version;
        }

        public String getZipFileName() {
            if (basename.equals(EAP)) {
                return  getFullVersionName() + ".zip";
            } else {
                return  getFullVersionName() + ".Final.zip";
            }
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public int getMicro() {
            return micro;
        }

        /**
         * Gets the maximum Java version under which a legacy host can properly
         * execute tests.
         */
        public int getMaxVMVersion() {
            return maxVM;
        }

        /**
         * Gets the minimum Java version under which a legacy host can properly
         * execute tests.
         */
        public int getMinVMVersion() {
            return minVM;
        }

        /**
         * Checks whether the current VM version exceeds the maximum version under which a legacy host can properly
         * execute tests. The check is disabled if system property "jboss.test.host.secondary.jvmhome" is set.
         */
        public void assumeMaxVM() {
            if (System.getProperty("jboss.test.host.secondary.jvmhome") == null) {
                String javaSpecVersion = System.getProperty("java.specification.version");
                int vm = "1.8".equals(javaSpecVersion) ? 8 : Integer.parseInt(javaSpecVersion);
                Assume.assumeFalse(vm > maxVM);
            }
        }

        public String getHostExclude() {
            return hostExclude;
        }

        public ModelVersion getModelVersion() {
            return modelVersion;
        }

        int compare(int major, int minor) {
            if (this.major < major) {
                return -1;
            }
            if (this.major > major) {
                return  1;
            }
            if (this.minor == minor) {
                return 0;
            }
            return this.minor < minor ? -1 : 1;
        }

        public Stability getStability() {
            return stability;
        }

        public String getDefaultSecondaryHostConfigFileName() {
            return useManagementRealms ? "secondary-config/host-secondary-mgmt-realm-security.xml" : "secondary-config/host-secondary.xml";
        }
    }
}
