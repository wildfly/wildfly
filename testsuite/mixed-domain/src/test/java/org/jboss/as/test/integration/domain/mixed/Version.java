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

    final String AS = "jboss-as-";
    final String WILDFLY = "wildfly";
    final String EAP = "jboss-eap-";

    enum AsVersion {
        AS_7_1_2_FINAL(AS, "7.1.2.Final"),
        AS_7_1_3_FINAL(AS, "7.1.3.Final"),
        AS_7_2_0_FINAL(AS, "7.2.0.Final");

        final String basename;
        final String version;

        AsVersion(String basename, String version){
            this.basename = basename;
            this.version = version;
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
    }


}
