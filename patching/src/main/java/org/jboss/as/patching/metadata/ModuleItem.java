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
 * @author Emanuel Muckenhuber
 */
public class ModuleItem extends ContentItem {

    public static final String MAIN_SLOT = "main";

    private final String slot;

    public ModuleItem(String name, String slot, byte[] contentHash) {
        this(name, slot, contentHash, ContentType.MODULE);
    }

    ModuleItem(String name, String slot, final byte[] contentHash, ContentType type) {
        super(name, contentHash, type);
        this.slot = slot == null ? MAIN_SLOT : slot;
    }

    /**
     * Get the module slot.
     *
     * @return the module slot
     */
    public String getSlot() {
        return slot;
    }

    @Override
    public String getRelativePath() {
        return getName() + ":" + slot;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(ModuleItem.class.getSimpleName()).append("{");
        builder.append(getName()).append(":").append(slot).append("}");
        return builder.toString();
    }
}
