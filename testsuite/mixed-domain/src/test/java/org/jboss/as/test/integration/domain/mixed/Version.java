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

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.TYPE)
public @interface Version {

    AsVersion value();

    String AS = "jboss-as-";
    String WILDFLY = "wildfly";
    String EAP = "jboss-eap-";

    enum AsVersion {
        EAP_6_2_0(EAP, 6, 2, 0),
        EAP_6_3_0(EAP, 6, 3, 0),
        EAP_6_4_0(EAP, 6, 4, 0),
        EAP_7_0_0(EAP, 7, 0, 0),
        EAP_7_1_0(EAP, 7, 1, 0);


        final String basename;
        private final int major;
        private final int minor;
        private final int micro;
        final String version;

        AsVersion(String basename, int major, int minor, int micro){
            this.basename = basename;
            this.major = major;
            this.minor = minor;
            this.micro = micro;
            this.version = major + "." + minor + "." + micro;
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
            return  getFullVersionName() + ".zip";
        }

        public boolean isEAP6Version() {
            return (this == EAP_6_2_0 || this == EAP_6_3_0 || this == EAP_6_4_0);
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
