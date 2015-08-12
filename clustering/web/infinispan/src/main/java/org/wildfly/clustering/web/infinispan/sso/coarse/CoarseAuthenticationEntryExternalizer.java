/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.sso.coarse;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValue;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueExternalizer;

/**
 * Externalizer for {@link CoarseAuthenticationEntry}.
 * @author Paul Ferraro
 * @param <A>
 * @param <D>
 */
public class CoarseAuthenticationEntryExternalizer<A, D, L> implements Externalizer<CoarseAuthenticationEntry<A, D, L>> {

    private final Externalizer<SimpleMarshalledValue<A>> externalizer = new SimpleMarshalledValueExternalizer<>();

    @Override
    public void writeObject(ObjectOutput output, CoarseAuthenticationEntry<A, D, L> entry) throws IOException {
        SimpleMarshalledValue<A> value = (SimpleMarshalledValue<A>) entry.getAuthentication();
        this.externalizer.writeObject(output, value);
    }

    @Override
    public CoarseAuthenticationEntry<A, D, L> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new CoarseAuthenticationEntry<>(this.externalizer.readObject(input));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class<CoarseAuthenticationEntry<A, D, L>> getTargetClass() {
        Class targetClass = CoarseAuthenticationEntry.class;
        return targetClass;
    }
}
