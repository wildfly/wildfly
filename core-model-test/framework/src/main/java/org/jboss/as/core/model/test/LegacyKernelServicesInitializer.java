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
package org.jboss.as.core.model.test;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface LegacyKernelServicesInitializer {

    String VERSION = "7.2.0.Alpha1-SNAPSHOT";

    LegacyKernelServicesInitializer initializerCreateModelResource(PathAddress parentAddress, PathElement relativeResourceAddress, ModelNode model);

    /**
     * If called, will not use boot operations rather ship across the model via ApplyRemoteMasterDomainHandler
     */
    LegacyKernelServicesInitializer setDontUseBootOperations();

    /**
     * The default is to validate the operations sent in to the model controller. Turn it off call this method
     *
     * @return this builder
     */
    LegacyKernelServicesInitializer setDontValidateOperations();


    public enum TestControllerVersion {
        MASTER ("org.jboss.as:jboss-as-host-controller:" + VERSION, null),
        V7_1_2_FINAL ("org.jboss.as:jboss-as-host-controller:7.1.2.Final", "7.1.2");

        String mavenGav;
        String testControllerVersion;
        private TestControllerVersion(String mavenGav, String testControllerVersion) {
            this.mavenGav = mavenGav;
            this.testControllerVersion = testControllerVersion;
        }

        String getLegacyControllerMavenGav() {
            return mavenGav;
        }

        String getTestControllerVersion() {
            return testControllerVersion;
        }
    }
}
