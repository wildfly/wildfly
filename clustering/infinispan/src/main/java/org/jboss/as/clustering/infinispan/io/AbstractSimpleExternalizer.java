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
package org.jboss.as.clustering.infinispan.io;

import java.util.Collections;
import java.util.Set;

/**
 * Abstract implementation of an externalizer for a single class.
 * @author Paul Ferraro
 */
@SuppressWarnings("serial")
// N.B. unlike JBoss Marshalling externalizers, Infinispan AdvancedExternalizers are never serialized
// While AdvancedExternalizer currently extends Serializable, this will be dropped in a future release
public abstract class AbstractSimpleExternalizer<T> implements SimpleExternalizer<T> {
    private final Class<T> targetClass;

    protected AbstractSimpleExternalizer(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public Set<Class<? extends T>> getTypeClasses() {
        return Collections.<Class<? extends T>>singleton(this.targetClass);
    }

    @Override
    public Integer getId() {
        // The externalizer ID will be auto-assigned during registration with cache manager
        return null;
    }
}
