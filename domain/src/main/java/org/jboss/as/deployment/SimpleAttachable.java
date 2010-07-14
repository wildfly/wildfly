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

package org.jboss.as.deployment;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of {@link Attachable} which may be used as a base class or on a standalone basis.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class SimpleAttachable implements Attachable {
    private final Map<AttachmentKey<?>, Object> attachments = new HashMap<AttachmentKey<?>, Object>();

    /** {@inheritDoc} */
    public <T> T getAttachment(final AttachmentKey<T> key) {
        if (key == null) {
            return null;
        }
        return key.getValueClass().cast(attachments.get(key));
    }

    /** {@inheritDoc} */
    public <T> T putAttachment(final AttachmentKey<T> key, final T value) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        return key.getValueClass().cast(attachments.put(key, key.getValueClass().cast(value)));
    }

    /** {@inheritDoc} */
    public <T> T removeAttachment(final AttachmentKey<T> key) {
        if (key == null) {
            return null;
        }
        return key.getValueClass().cast(attachments.get(key));
    }
}
