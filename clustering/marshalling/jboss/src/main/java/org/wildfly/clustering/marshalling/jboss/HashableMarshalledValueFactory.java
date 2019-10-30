/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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


/**
 * Factory for creating a {@link HashableMarshalledValue}.
 * @author Paul Ferraro
 */
public class HashableMarshalledValueFactory extends SimpleMarshalledValueFactory {

    public HashableMarshalledValueFactory(MarshallingContextFactory factory, MarshallingConfigurationRepository repository, ClassLoader loader) {
        super(factory, repository, loader);
    }

    public HashableMarshalledValueFactory(MarshallingContext context) {
        super(context);
    }

    @Override
    public <T> SimpleMarshalledValue<T> createMarshalledValue(T object) {
        return new HashableMarshalledValue<>(object, this.context);
    }
}
