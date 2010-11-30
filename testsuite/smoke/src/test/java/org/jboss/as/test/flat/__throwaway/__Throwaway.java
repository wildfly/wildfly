/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.flat.__throwaway;

import javax.transaction.TransactionManager;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.MultiplePathFilterBuilder;
import org.jboss.modules.PathFilter;
import org.jboss.modules.PathFilters;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class __Throwaway {

    public static void main(String[] args) {
        System.setProperty("module.path", "/Users/kabir/sourcecontrol/jboss-as7/git/jboss-as/build/target/jboss-7.0.0.Alpha2/modules");
        System.clearProperty("java.class.path");

        testFilters();

        System.out.println(TransactionManager.class.getClassLoader());
        loadModule("__test.api", TransactionManager.class.getName());
        loadModule("__test.user", TransactionManager.class.getName());
    }

    private static void testFilters() {
        MultiplePathFilterBuilder builder = PathFilters.multiplePathFilterBuilder(true);
        builder.addFilter(PathFilters.match("javax/blah"), true);
        //builder.addFilter(PathFilters.match("javax/blah/**"), true);
        builder.addFilter(PathFilters.match("javax/**"), false);

        PathFilter filter = builder.create();
        System.out.println("accept javax/blah: " + filter.accept("javax/blah"));
        System.out.println("accept javax/blah/sub: " + filter.accept("javax/blah/sub"));
        System.out.println("accept javax/notthere: " + filter.accept("javax/notthere"));
    }



    private static void loadModule(String moduleName, String classname) {
        System.out.println("\n---------- " + moduleName + " : " + classname);
        try {
            Module module = Module.getDefaultModuleLoader().loadModule(ModuleIdentifier.fromString(moduleName));
            try {
                Class<?> clazz = module.getClassLoader().loadClass(classname);
                System.out.println("Loaded using " + clazz.getClassLoader());
            } catch (ClassNotFoundException e) {
                System.out.println("Could not load class from " + moduleName);
            }
        } catch (ModuleLoadException e) {
            System.out.println("Could not load module " + moduleName);
        }
    }
}
