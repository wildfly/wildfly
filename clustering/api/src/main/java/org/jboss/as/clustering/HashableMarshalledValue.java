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

package org.jboss.as.clustering;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Like {@link SimpleMarshalledValue}, but also serializes the underlying object's hash code,
 * so that this object can still be hashed, even if deserialized, but not yet rehydrated.
 * @author Paul Ferraro
 */
public class HashableMarshalledValue<T> extends SimpleMarshalledValue<T> {
    private static final long serialVersionUID = -7576022002375288323L;

    private transient int hashCode;

    /**
     * @param object
     * @param context
     * @throws IOException
     */
    public HashableMarshalledValue(T object, MarshallingContext context) throws IOException {
        super(object, context);
        this.hashCode = (object != null ) ? object.hashCode() : 0;
    }

    public HashableMarshalledValue() {
        // Required for externalization
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object object) {
        if ((object != null) && !(object instanceof HashableMarshalledValue)) {
            HashableMarshalledValue<?> value = (HashableMarshalledValue<?>) object;
            return (this.hashCode == value.hashCode()) && super.equals(object);
        }
        return super.equals(object);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(this.hashCode);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        super.readExternal(in);
        this.hashCode = in.readInt();
    }
}
