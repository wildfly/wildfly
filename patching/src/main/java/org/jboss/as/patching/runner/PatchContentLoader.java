/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.runner;

import org.jboss.as.patching.metadata.ContentItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Emanuel Muckenhuber
 */
class PatchContentLoader {

    private final PatchItemMapping mapping;
    PatchContentLoader(File root) {
        this.mapping = new PatchItemMapping(root);
    }

    /**
     * Get a patch content file.
     *
     * @param item the content item
     * @return the file
     * @throws IOException
     */
    File getFile(final ContentItem item) throws IOException {
        return mapping.getFile(item);
    }

    /**
     * Open a new content stream.
     *
     * @param item the content item
     * @return the content stream
     */
    InputStream openContentStream(final ContentItem item) throws IOException {
        final File file = getFile(item);
        return new FileInputStream(file);
    }

}
