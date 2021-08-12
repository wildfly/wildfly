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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.as.controller.ModelVersion;
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
        EAP_7_4_0(EAP, 7, 4, 0, 11, 8, "EAP7.4", ModelVersion.create(16, 0)),
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

        /**
         * Metadata related to the server version we are using as slave
         * @param basename Base name of the server, used to locate the zip file that contains the slave under test.
         * @param major Major release number
         * @param minor Minor release number
         * @param micro Micro release number
         * @param maxVM The maximum Java version under which a legacy host can properly execute tests
         * @param minVM The minimum Java version under which a legacy host can properly execute tests
         * @param hostExclude The host-exclude name that represents this slave
         * @param modelVersion The Kernel version of this slave
         */
        AsVersion(String basename, int major, int minor, int micro, int maxVM, int minVM, String hostExclude, ModelVersion modelVersion){
            this.basename = basename;
            this.major = major;
            this.minor = minor;
            this.micro = micro;
            this.version = major + "." + minor + "." + micro;
            this.maxVM = maxVM;
            this.minVM = minVM;
            this.hostExclude = hostExclude;
            this.modelVersion = modelVersion;
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
         * execute tests. The check is disabled if system property "jboss.test.host.slave.jvmhome" is set.
         */
        public void assumeMaxVM() {
            if (System.getProperty("jboss.test.host.slave.jvmhome") == null) {
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
    }
}
