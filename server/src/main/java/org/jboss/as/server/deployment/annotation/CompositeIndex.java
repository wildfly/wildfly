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

package org.jboss.as.server.deployment.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

/**
 * Composite annotation index.  Represents an annotation index for an entire deployment.
 *
 * @author John Bailey
 */
public class CompositeIndex {
    final Collection<Index> indexes;

    public CompositeIndex(final Collection<Index> indexes) {
        this.indexes = indexes;
    }

    /**
     * @see {@link Index#getAnnotations(org.jboss.jandex.DotName)}
     */
    public List<AnnotationInstance> getAnnotations(final DotName annotationName) {
        final List<AnnotationInstance> allInstances = new ArrayList<AnnotationInstance>();
        for (Index index : indexes) {
            final List<AnnotationInstance> list = index.getAnnotations(annotationName);
            if (list != null) {
                allInstances.addAll(list);
            }
        }
        return Collections.unmodifiableList(allInstances);
    }

    /**
     * @see {@link Index#getKnownSubclasses(org.jboss.jandex.DotName)}
     */
    public List<ClassInfo> getKnownSubclasses(final DotName className) {
        final List<ClassInfo> allKnown = new ArrayList<ClassInfo>();
        for (Index index : indexes) {
            final List<ClassInfo> list = index.getKnownSubclasses(className);
            if (list != null) {
                allKnown.addAll(list);
            }
        }
        return Collections.unmodifiableList(allKnown);
    }

    /**
     * @see {@link Index#getClassByName(org.jboss.jandex.DotName)}
     */
    public ClassInfo getClassByName(final DotName className) {
        for (Index index : indexes) {
            final ClassInfo info = index.getClassByName(className);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    /**
     * @see {@link org.jboss.jandex.Index#getKnownClasses()}
     */
    public Collection<ClassInfo> getKnownClasses() {
        final List<ClassInfo> allKnown = new ArrayList<ClassInfo>();
        for (Index index : indexes) {
            final Collection<ClassInfo> list = index.getKnownClasses();
            if (list != null) {
                allKnown.addAll(list);
            }
        }
        return Collections.unmodifiableCollection(allKnown);
    }
}
