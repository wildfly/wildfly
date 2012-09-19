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

/**
 * A modification of a content item. The {@linkplain ModificationType} describes whether the content
 * is added, modified or removed.
 *
 * @author Emanuel Muckenhuber
 */
public class ContentModification {

    private final ContentItem item;
    private final byte[] targetHash;
    private final ModificationType type;

    public ContentModification(ContentItem item, byte[] targetHash, ModificationType type) {
        this.item = item;
        this.targetHash = targetHash;
        this.type = type;
    }

    public ContentModification(ContentItem item, ContentModification existing) {
        this(item, existing.getTargetHash(), existing.getType());
    }

    public ContentItem getItem() {
        return item;
    }

    public <T extends ContentItem> T getItem(Class<T> expected) {
        return expected.cast(item);
    }

    public byte[] getTargetHash() {
        return targetHash;
    }

    public ModificationType getType() {
        return type;
    }

}
