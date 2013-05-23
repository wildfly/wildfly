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

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.patching.metadata.ContentItem;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchingException extends Exception {

    private final Collection<ContentItem> conflicts;

    public PatchingException() {
        super("patching exception");
        conflicts = Collections.emptyList();
    }

    public PatchingException(Collection<ContentItem> conflicts) {
        // FIXME message is mandatory to wrap it into a OperationFailedException
        super("Conficts detected: " + conflicts.toString());
        this.conflicts = conflicts;
    }

    public PatchingException(String message) {
        super(message);
        this.conflicts = Collections.emptyList();
    }

    public PatchingException(String message, Throwable cause) {
        super(message, cause);
        this.conflicts = Collections.emptyList();
    }

    public PatchingException(Throwable cause) {
        super(cause);
        this.conflicts = Collections.emptyList();
    }

    public PatchingException(String format, Object... args) {
        this(String.format(format, args));
    }

    public PatchingException(Throwable cause, String format, Object... args) {
        this(String.format(format, args), cause);
    }

    /**
     * Get the conflicting content items.
     *
     * @return the conflicting content items
     */
    public Collection<ContentItem> getConflicts() {
        return conflicts;
    }

    /**
     * Check if there content-item conflicts.
     *
     * @return
     */
    public boolean hasConflicts() {
        return ! conflicts.isEmpty();
    }

}
