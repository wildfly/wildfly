/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.context;

import javax.naming.RefAddr;
import javax.naming.Reference;
import org.jboss.modules.Module;

/**
 * Reference implementation that captures a module name and allows object factories to be loaded and created from
 * modules.
 *
 * @author John Bailey
 */
public class ModularReference extends Reference {
    private static final long serialVersionUID = -4805781394834948096L;
    private final String moduleName;

    /**
     * Create a ModuleReference from a target type and factory class.
     *
     * @param type The class type for the reference
     * @param factoryClass The factory class
     * @return A ModularReference
     */
    public static ModularReference create(final Class<?> type, final Class<?> factoryClass) {
        return create(type.getName(), factoryClass);
    }

    /**
     * Create a ModuleReference from a target class name and factory class.
     *
     * @param className The class name for the reference
     * @param factoryClass The factory class
     * @return A ModularReference
     */
    public static ModularReference create(final String className, final Class<?> factoryClass) {
        return new ModularReference(className, factoryClass.getName(), Module.forClass(factoryClass).getName());
    }


    /**
     * Create a ModuleReference from a target type, reference address and factory class.
     *
     * @param type The class type for the reference
     * @param addr The address of the object
     * @param factoryClass The factory class
     * @return A ModularReference
     */
    public static ModularReference create(final Class<?> type, final RefAddr addr, final Class<?> factoryClass) {
        return create(type.getName(), addr, factoryClass);
    }

    /**
     * Create a ModuleReference from a target class name, reference address and factory class.
     *
     * @param className The class name for the reference
     * @param addr The address of the object
     * @param factoryClass The factory class
     * @return A ModularReference
     */
    public static ModularReference create(final String className, final RefAddr addr, final Class<?> factoryClass) {
        return new ModularReference(className, addr, factoryClass.getName(), Module.forClass(factoryClass).getName());
    }

    /**
     * Create an instance.
     *
     * @param className The class name of the target object type
     * @param factory The object factory class name
     * @param moduleName The module name to load the factory class
     */
    private ModularReference(final String className, final String factory, final String moduleName) {
        super(className, factory, null);
        this.moduleName = moduleName;
    }

    /**
     * Create an instance.
     *
     * @param className The class name of the target object type
     * @param addr The address of the object
     * @param factory The object factory class name
     * @param moduleName The module name to load the factory class
     */
    private ModularReference(final String className, final RefAddr addr, final String factory, final String moduleName) {
        super(className, addr, factory, null);
        this.moduleName = moduleName;
    }

    /**
     * Get the module name to load the factory class from.
     *
     * @return The module name
     */
    public String getModuleName() {
        return moduleName;
    }
}
