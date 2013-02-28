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

package org.jboss.as.util.security;

import java.io.File;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;

/**
 * A security action to create a temporary file.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class CreateTempFileAction implements PrivilegedExceptionAction<File> {

    private final String prefix;
    private final String suffix;
    private final File directory;

    /**
     * Construct a new instance.
     *
     * @param prefix the prefix to set
     * @param suffix the suffix to set
     * @param directory the directory
     */
    public CreateTempFileAction(final String prefix, final String suffix, final File directory) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.directory = directory;
    }

    /**
     * Construct a new instance.
     *
     * @param prefix the prefix to set
     * @param suffix the suffix to set
     */
    public CreateTempFileAction(final String suffix, final String prefix) {
        this(prefix, suffix, null);
    }

    public File run() throws IOException {
        return File.createTempFile(prefix, suffix, directory);
    }
}
