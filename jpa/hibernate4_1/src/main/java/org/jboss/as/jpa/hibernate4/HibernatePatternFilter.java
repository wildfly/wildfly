/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate4;

import static org.jipijapa.JipiLogger.JPA_LOGGER;

import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * Mock work of NativeScanner matching.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Scott Marlow
 */
public class HibernatePatternFilter implements VirtualFileFilter {
    private static final String SLASH = "/";
    private static final String PREFIX = "**/*";
    private final String pattern;
    private final boolean exact;

    public HibernatePatternFilter(String pattern) {
        if (pattern == null)
            throw JPA_LOGGER.nullVar("pattern");

        exact = !pattern.contains(SLASH); // no path split or glob
        if (exact == false && (pattern.startsWith(PREFIX))) {
            this.pattern = pattern.substring(4);
        } else {
            this.pattern = pattern;
        }
    }

    protected boolean accepts(String name) {
        return exact ? name.equals(pattern) : name.endsWith(pattern);
    }

    public boolean accepts(VirtualFile file) {
        String name = exact ? file.getName() : file.getPathName();
        return accepts(name);
    }

}
