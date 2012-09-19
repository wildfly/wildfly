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
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;

/**
 * @author Emanuel Muckenhuber
 */
class Location {

    private final ContentItem item;
    private final int hashCode;

    Location(ContentItem item) {
        this.item = item;
        this.hashCode = hashCode(item);
    }

    public ContentItem getItem() {
        return item;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Location location = (Location) o;
        //
        return hashCode == location.hashCode;
    }

    static int hashCode(final ContentItem item) {
        final ContentType type = item.getContentType();
        switch (type) {
            case MODULE:
            case BUNDLE:
                final ModuleItem module = (ModuleItem) item;
                final String[] path = module.getName().split("\\.");
                return hashCode(type.toString(), module.getSlot(), path);
            case MISC:
                final MiscContentItem misc = (MiscContentItem) item;
                return hashCode(type.toString(), misc.getName(), misc.getPath());
            default:
                throw new IllegalStateException();
        }
    }

    static int hashCode(final String root, final String name, final String... path) {
        int hash = root.hashCode();
        for(final String p : path) {
            hash = 31 * hash + p.hashCode();
        }
        hash = 31 * hash + name.hashCode();
        return hash;
    }

}
