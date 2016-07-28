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
package org.wildfly.clustering.web.infinispan.sso;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValue;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueExternalizer;

/**
 * Externalizer for {@link AuthenticationEntry}.
 * @author Paul Ferraro
 * @param <A>
 * @param <D>
 */
public class AuthenticationEntryExternalizer<A, L> implements Externalizer<AuthenticationEntry<A, L>> {

    private final Externalizer<SimpleMarshalledValue<A>> externalizer = new SimpleMarshalledValueExternalizer<>();

    @Override
    public void writeObject(ObjectOutput output, AuthenticationEntry<A, L> entry) throws IOException {
        SimpleMarshalledValue<A> value = (SimpleMarshalledValue<A>) entry.getAuthentication();
        this.externalizer.writeObject(output, value);
    }

    @Override
    public AuthenticationEntry<A, L> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new AuthenticationEntry<>(this.externalizer.readObject(input));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class<AuthenticationEntry<A, L>> getTargetClass() {
        Class targetClass = AuthenticationEntry.class;
        return targetClass;
    }
}
