/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.shared;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.test.module.util.TestModule;
import org.jboss.shrinkwrap.api.spec.JavaArchive;


public class ModuleUtils {

    private static final String[] EE_DEPENDENCIES = new String[] {"javax.enterprise.api", "javax.inject.api", "javax.servlet.api", "javax.servlet.jsp.api"};
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
