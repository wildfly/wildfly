/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.embedded.ejb3;

import org.jboss.vfs.VirtualFile;

/**
 * Defines a mechanism whereby ClassPath entries
 * may be excluded from scanning for EJB resources
 * according to some implementation-specific
 * criteria.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public interface ExclusionFilter {
    /**
     * Returns whether this {@link org.jboss.vfs.VirtualFile} should be
     * excluded from scanning for EJB resources.  The criteria
     * whereby a file is excluded is up to the implementation.
     *
     * @param file The file to inspect for exclusion properties
     * @throws IllegalArgumentException If the file is not specified
     */
    boolean exclude(VirtualFile file) throws IllegalArgumentException;

}
