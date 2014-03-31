/*
 * Copyright (C) 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file
 * in the distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.model.test;

import org.jboss.modules.filter.ClassFilter;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class SingleClassFilter implements ClassFilter {

    private final String filteredClassName;

    SingleClassFilter(String filteredClassName) {
        this.filteredClassName = filteredClassName;
    }

    @Override
    public boolean accept(String className) {
        return className != null && className.startsWith(this.filteredClassName);
    }

    public static ClassFilter createFilter(Class<?> filteredClass) {
        return new SingleClassFilter(filteredClass.getName());
    }

}
