/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment;

import java.util.List;

/**
 * An entity which can contain attachments.
 *
 */
public interface Attachable {

    /**
     * Check if an attachment exists for this key.
     *
     * @param key the attachment key
     * @return {@code false} if there is none
     */
    boolean hasAttachment(AttachmentKey<?> key);

    /**
     * Get an attachment value.  If no attachment exists for this key, {@code null} is returned.
     *
     * @param key the attachment key
     * @param <T> the value type
     * @return the value, or {@code null} if there is none
     */
    <T> T getAttachment(AttachmentKey<T> key);

    /**
     * Gets a list attachment value. If not attachment exists for this key an empty list is returned
     *
     * @param <T> the value type
     * @param key the attachment key
     * @return the value, or an empty list if there is none
     */
    <T> List<T> getAttachmentList(AttachmentKey<? extends List<T>> key);

    /**
     * Set an attachment value. If an attachment for this key was already set, return the original value. If the value being set
     * is {@code null}, the attachment key is removed.
     *
     * @param key the attachment key
     * @param value the new value
     * @param <T> the value type
     * @return the old value, or {@code null} if there was none
     */
    <T> T putAttachment(AttachmentKey<T> key, T value);

    /**
     * Remove an attachment, returning its previous value.
     *
     * @param key the attachment key
     * @param <T> the value type
     * @return the old value, or {@code null} if there was none
     */
    <T> T removeAttachment(AttachmentKey<T> key);

    /**
     * Add a value to a list-typed attachment key.  If the key is not mapped, add such a mapping.
     *
     * @param key the attachment key
     * @param value the value to add
     * @param <T> the list value type
     */
    <T> void addToAttachmentList(AttachmentKey<AttachmentList<T>> key, T value);
}
