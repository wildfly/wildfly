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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Emanuel Muckenhuber
 */
public interface PatchContentLoader {

    /**
     * Open a new content stream.
     *
     * @param item the content item
     * @return the content stream
     */
    InputStream openContentStream(ContentItem item) throws IOException;

    public static class FilePatchContentLoader implements PatchContentLoader {

        private final File root;
        protected FilePatchContentLoader(File content) {
            this.root = content;
        }

        @Override
        public InputStream openContentStream(ContentItem item) throws IOException {
            File file = root;
            for(final String path : item.getPath()) {
                file = new File(file, path);
            }
            file = new File(file, item.getName());
            return new FileInputStream(file);
        }

        public static PatchContentLoader create(final File content) {
            return new FilePatchContentLoader(content);
        }

    }

}
