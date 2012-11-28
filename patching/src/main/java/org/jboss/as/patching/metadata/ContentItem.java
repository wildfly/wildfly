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

package org.jboss.as.patching.metadata;

import java.util.Arrays;

/**
 * Base content item.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class ContentItem {

    private final String name;
    private final byte[] contentHash;
    private final ContentType contentType;

    protected ContentItem(String name, byte[] contentHash, ContentType contentType) {
        this.name = name;
        this.contentHash = contentHash;
        this.contentType = contentType;
    }

    /**
     * Get the content hash.
     *
     * @return the content hash
     */
    public byte[] getContentHash() {
        return contentHash;
    }

    /**
     * Get the content name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The content type.
     *
     * @return the content type
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * Get a relative path or module description.
     *
     * @return the path description
     */
    public abstract String getRelativePath();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(contentHash);
        result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ContentItem other = (ContentItem) obj;
        if (!Arrays.equals(contentHash, other.contentHash))
            return false;
        if (contentType != other.contentType)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
