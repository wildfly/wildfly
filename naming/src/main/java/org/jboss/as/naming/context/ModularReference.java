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

package org.jboss.as.naming.context;

import javax.naming.Reference;

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
     * Create an instance.
     *
     * @param className The class name of the target object type
     * @param factory The object factory class name
     * @param moduleName The module name to load the factroy class
     */
    public ModularReference(final String className, final String factory, final String moduleName) {
        super(className, factory, null);
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
