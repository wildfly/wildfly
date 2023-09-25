/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.context;

import javax.naming.RefAddr;
import javax.naming.Reference;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;

/**
 * Reference implementation that captures a module name and allows object factories to be loaded and created from
 * modules.
 *
 * @author John Bailey
 */
public class ModularReference extends Reference {
    private static final long serialVersionUID = -4805781394834948096L;
    private final ModuleIdentifier moduleIdentifier;

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
        return new ModularReference(className, factoryClass.getName(), Module.forClass(factoryClass).getIdentifier());
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
        return new ModularReference(className, addr, factoryClass.getName(), Module.forClass(factoryClass).getIdentifier());
    }

    /**
     * Create an instance.
     *
     * @param className The class name of the target object type
     * @param factory The object factory class name
     * @param moduleIdentifier The module name to load the factory class
     */
    public ModularReference(final String className, final String factory, final ModuleIdentifier moduleIdentifier) {
        super(className, factory, null);
        this.moduleIdentifier = moduleIdentifier;
    }

    /**
     * Create an instance.
     *
     * @param className The class name of the target object type
     * @param addr The address of the object
     * @param factory The object factory class name
     * @param moduleIdentifier The module name to load the factory class
     */
    public ModularReference(final String className, final RefAddr addr, final String factory, final ModuleIdentifier moduleIdentifier) {
        super(className, addr, factory, null);
        this.moduleIdentifier = moduleIdentifier;
    }

    /**
     * Get the module name to load the factory class from.
     *
     * @return The module name
     */
    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }
}
