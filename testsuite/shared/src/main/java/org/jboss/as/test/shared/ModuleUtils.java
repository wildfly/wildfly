/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.Extension;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.shrinkwrap.api.spec.JavaArchive;


public class ModuleUtils {

    public static final String[] EE_DEPENDENCIES = new String[] {"javax.enterprise.api", "javax.inject.api", "javax.servlet.api", "javax.servlet.jsp.api"};
    public static TestModule createTestModuleWithEEDependencies(String moduleName) {
        TestModule testModule = new TestModule("test." + moduleName, EE_DEPENDENCIES);
        return testModule;
    }

    public static final TestModule.ClassCallback ENTERPRISE_INJECT = new TestModule.ClassCallback() {
        @Override
        public void classesAdded(JavaArchive jar, List<Class<?>> classes) {
            List<Class<Extension>> extensions = new ArrayList<Class<Extension>>(1);
            for (Class<?> clazz : classes) {
                if (Extension.class.isAssignableFrom(clazz)) {
                    extensions.add((Class<Extension>) clazz);
                }
            }

            if (!extensions.isEmpty()) {
                Class<Extension>[] a = (Class<Extension>[]) Array.newInstance(Extension.class.getClass(), 0);
                jar.addAsServiceProvider(Extension.class, extensions.toArray(a));
            }
        }
    };
}
