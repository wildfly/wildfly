/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import org.infinispan.protostream.ImmutableSerializationContext;

/**
 * Common interface of {@link ProtoStreamReader} and {@link ProtoStreamWriter}.
 * @author Paul Ferraro
 */
public interface ProtoStreamOperation {

    interface Context {
        /**
         * Records a the specified object reference, in case it is referenced again within a stream.
         * This method is idempotent.
         * @param object an object reference
         */
        void addReference(Object object);
    }

    /**
     * Returns the serialization context of the associated marshaller.
     * @return an immutable serialization context
     */
    ImmutableSerializationContext getSerializationContext();
}
