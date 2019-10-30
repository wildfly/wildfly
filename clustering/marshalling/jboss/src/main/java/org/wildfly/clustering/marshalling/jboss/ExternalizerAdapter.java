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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Adapts a {@link org.wildfly.clustering.marshalling.Externalizer} to a JBoss Marshalling {@link org.jboss.marshalling.Externalizer}.
 * @author Paul Ferraro
 */
public class ExternalizerAdapter implements org.jboss.marshalling.Externalizer {
    private static final long serialVersionUID = 1714120446322944436L;

    private final Externalizer<Object> externalizer;

    @SuppressWarnings("unchecked")
    public ExternalizerAdapter(Externalizer<?> externalizer) {
        this.externalizer = (Externalizer<Object>) externalizer;
    }

    @Override
    public void writeExternal(Object subject, ObjectOutput output) throws IOException {
        this.externalizer.writeObject(output, subject);
    }

    @Override
    public Object createExternal(Class<?> subjectType, ObjectInput input) throws IOException, ClassNotFoundException {
        return this.externalizer.readObject(input);
    }
}
