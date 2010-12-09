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
import org.jboss.modules.PathFilter;

/**
 * @author John E. Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleDependency implements Serializable {

    private static final long serialVersionUID = 2749276798703740853L;

    private final ModuleIdentifier identifier;
    private final boolean export;
    private final boolean optional;
    private final List<PathFilter> importFilters = new ArrayList<PathFilter>();
    private final List<PathFilter> exportFilters = new ArrayList<PathFilter>();

    public ModuleDependency(final ModuleIdentifier identifier, final boolean optional, final boolean export) {
        this.identifier = identifier;
        this.optional = optional;
        this.export = export;
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

    public List<PathFilter> getImportFilters() {
        return importFilters;
    }

    public List<PathFilter> getExportFilters() {
        return exportFilters;
    }
}
