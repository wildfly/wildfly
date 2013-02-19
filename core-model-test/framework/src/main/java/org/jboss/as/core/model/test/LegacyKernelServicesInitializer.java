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

import java.util.Properties;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface LegacyKernelServicesInitializer {

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


    /**
     * By default the {@link KernelServicesBuilder#build()} method will use the boot operations passed into the
     * legacy controller and try to boot up the current controller with those. This is for checking that e.g. cli scripts written
     * against the legacy controller still work with the current one. To turn this check off call this method.
     *
     * @return this initializer
     */
    LegacyKernelServicesInitializer skipReverseControllerCheck();

    /**
     * By default the {@link KernelServicesBuilder#build()} method will use the boot operations passed into the
     * legacy controller and try to boot up the current controller with those. This is for checking that e.g. cli scripts written
     * against the legacy controller still work with the current one. Configure how the models are compared with this method
     *
     * @param mainModelFixer a model fixer to fix up the main model before comparison
     * @param legacyModelFixer a model fixer to fix up the model created from legacy boot operations before comparison
     * @return this initializer
     */
    LegacyKernelServicesInitializer configureReverseControllerCheck(ModelFixer mainModelFixer, ModelFixer legacyModelFixer);

    public enum TestControllerVersion {
        MASTER("org.jboss.as:jboss-as-host-controller:" + VersionLocator.getCurrentVersion(), null),
        V7_1_2_FINAL("org.jboss.as:jboss-as-host-controller:7.1.2.Final", "7.1.2"),
        V7_1_3_FINAL("org.jboss.as:jboss-as-host-controller:7.1.3.Final", "7.1.3");

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

    static final class VersionLocator {
        private static String VERSION;

        static {
            try {
                Properties props = new Properties();
                props.load(LegacyKernelServicesInitializer.class.getResourceAsStream("version.properties"));
                VERSION = props.getProperty("as.version");
            } catch (Exception e) {
                VERSION = "8.0.0.Alpha1-SNAPSHOT";
                e.printStackTrace();
            }
        }

        static String getCurrentVersion() {
            return VERSION;
        }
    }
}
