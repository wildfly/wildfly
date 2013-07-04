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

package org.jboss.as.patching;

import java.util.Collection;

import org.jboss.as.patching.metadata.ContentItem;

/**
 * The exception is thrown when a patch could not be applied or
 * rolled back because of the content conflicts.
 * E.g. if the actual hash codes of the content on the file system
 * don't match the expected values.
 *
 * @author Alexey Loubyansky
 */
public class ContentConflictsException extends PatchingException {

    private static final long serialVersionUID = -6654143665639437807L;

    private final Collection<ContentItem> conflicts;

    public ContentConflictsException(Collection<ContentItem> conflicts) {
        this("Conflicts detected", conflicts);
    }

    public ContentConflictsException(String msg, Collection<ContentItem> conflicts) {
        super(msg + ": " + conflicts);
        this.conflicts = conflicts;
    }

    public Collection<ContentItem> getConflicts() {
        return conflicts;
    }
}
