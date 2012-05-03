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

package org.jboss.as.clustering.ejb3.cache.backing.infinispan;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.clustering.infinispan.io.AbstractSimpleExternalizer;
import org.jboss.ejb.client.BasicSessionID;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UnknownSessionID;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("serial")
public class SessionIDExternalizer extends AbstractSimpleExternalizer<SessionID> {

    public SessionIDExternalizer() {
        super(SessionID.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Class<? extends SessionID>> getTypeClasses() {
        return new HashSet<Class<? extends SessionID>>(Arrays.asList(BasicSessionID.class, UnknownSessionID.class));
    }

    @Override
    public void writeObject(ObjectOutput output, SessionID id) throws IOException {
        byte[] encoded = id.getEncodedForm();
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    @Override
    public SessionID readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        byte[] encoded = new byte[input.readInt()];
        input.read(encoded);
        return SessionID.createSessionID(encoded);
    }
}
