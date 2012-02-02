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

package org.jboss.as.server.deployment.module;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilter;

/**
 * @author John E. Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleDependency implements Serializable {

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ModuleDependency [");
        if (identifier != null)
            builder.append("identifier=").append(identifier).append(", ");
        if (moduleLoader != null)
            builder.append("moduleLoader=").append(moduleLoader).append(", ");
        builder.append("export=").append(export).append(", optional=").append(optional).append(", importServices=").append(
                importServices).append("]");
        return builder.toString();
    }

    private static final long serialVersionUID = 2749276798703740853L;

    private final ModuleLoader moduleLoader;
    private final ModuleIdentifier identifier;
    private final boolean export;
    private final boolean optional;
    private final List<FilterSpecification> importFilters = new ArrayList<FilterSpecification>();
    private final List<FilterSpecification> exportFilters = new ArrayList<FilterSpecification>();
    private final boolean importServices;
    private final boolean userSpecified;

    /**
     * Construct a new instance.
     *
     * @param moduleLoader the module loader of the dependency (if {@code null}, then use the default server module loader)
     * @param identifier the module identifier
     * @param optional {@code true} if this is an optional dependency
     * @param export {@code true} if resources should be exported by default
     * @param importServices
     * @param userSpecified {@code true} if this dependency was specified by the user, {@code false} if it was automatically added
     */
    public ModuleDependency(final ModuleLoader moduleLoader, final ModuleIdentifier identifier, final boolean optional, final boolean export, final boolean importServices, final boolean userSpecified) {
        this.identifier = identifier;
        this.optional = optional;
        this.export = export;
        this.moduleLoader = moduleLoader;
        this.importServices = importServices;
        this.userSpecified = userSpecified;
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isExport() {
        return export;
    }

    public boolean isUserSpecified() {
        return userSpecified;
    }

    public void addImportFilter(PathFilter pathFilter, boolean include) {
        importFilters.add(new FilterSpecification(pathFilter, include));
    }

    public List<FilterSpecification> getImportFilters() {
        return importFilters;
    }

    public void addExportFilter(PathFilter pathFilter, boolean include) {
        exportFilters.add(new FilterSpecification(pathFilter, include));
    }

    public List<FilterSpecification> getExportFilters() {
        return exportFilters;
    }

    public boolean isImportServices() {
        return importServices;
    }
}
