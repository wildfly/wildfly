/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.marshalling.jboss;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * {@link org.jboss.marshalling.ClassTable} implementation that dynamically loads {@link ClassTableContributor} instances visible from a given {@link ClassLoader}.
 * @author Paul Ferraro
 */
public class DynamicClassTable extends SimpleClassTable {

    public DynamicClassTable(ClassLoader loader) {
        super(findClasses(loader));
    }

    private static Class<?>[] findClasses(ClassLoader loader) {
        List<Class<?>> classes = new LinkedList<>();
        classes.add(Serializable.class);
        classes.add(Externalizable.class);
        classes.add(MarshalledValue.class);
        classes.add(SimpleMarshalledValue.class);
        classes.add(HashableMarshalledValue.class);
        ServiceLoader.load(ClassTableContributor.class, loader).forEach(contributor -> classes.addAll(contributor.getKnownClasses()));
        return classes.toArray(new Class<?>[classes.size()]);
    }
}
