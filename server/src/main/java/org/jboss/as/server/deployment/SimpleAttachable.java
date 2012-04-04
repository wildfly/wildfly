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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.ServerMessages;

/**
 * A simple implementation of {@link Attachable} which may be used as a base class or on a standalone basis.
 * <p>
 * This class is thread safe, as all methods are synchronized.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class SimpleAttachable implements Attachable {
    private final Map<AttachmentKey<?>, Object> attachments = new HashMap<AttachmentKey<?>, Object>();

    /** {@inheritDoc} */
    public synchronized boolean hasAttachment(AttachmentKey<?> key) {
        if (key == null) {
            return false;
        }
        return attachments.containsKey(key);
    }

    /** {@inheritDoc} */
    public synchronized <T> T getAttachment(final AttachmentKey<T> key) {
        if (key == null) {
            return null;
        }
        return key.cast(attachments.get(key));
    }

    /** {@inheritDoc} */
    public synchronized <T> List<T> getAttachmentList(AttachmentKey<? extends List<T>> key) {
        if (key == null) {
            return null;
        }
        List<T> list = key.cast(attachments.get(key));
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    /** {@inheritDoc} */
    public synchronized <T> T putAttachment(final AttachmentKey<T> key, final T value) {
        if (key == null) {
            throw ServerMessages.MESSAGES.nullAttachmentKey();
        }
        return key.cast(attachments.put(key, key.cast(value)));
    }

    /** {@inheritDoc} */
    public synchronized <T> T removeAttachment(final AttachmentKey<T> key) {
        if (key == null) {
            return null;
        }
        return key.cast(attachments.remove(key));
    }

    /** {@inheritDoc} */
    public synchronized <T> void addToAttachmentList(final AttachmentKey<AttachmentList<T>> key, final T value) {
        if (key != null) {
            final Map<AttachmentKey<?>, Object> attachments = this.attachments;
            final AttachmentList<T> list = key.cast(attachments.get(key));
            if (list == null) {
                final AttachmentList<T> newList = new AttachmentList<T>(((ListAttachmentKey<T>) key).getValueClass());
                attachments.put(key, newList);
                newList.add(value);
            } else {
                list.add(value);
            }
        }
    }
}
