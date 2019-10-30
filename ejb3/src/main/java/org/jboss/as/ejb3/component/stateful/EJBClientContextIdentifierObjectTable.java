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

package org.jboss.as.ejb3.component.stateful;

import org.jboss.ejb.client.EJBClient;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;

import java.io.IOException;

/**
 * By default, EJB proxies don't serialize the {@link org.jboss.ejb.client.EJBClientContextIdentifier} associated with them,
 * so this {@link ObjectTable} marshals such EJB proxies to serializable even the {@link org.jboss.ejb.client.EJBClientContextIdentifier} (if any)
 * associated with that proxy.
 *
 * @author Jaikiran Pai
 */
@Deprecated
public class EJBClientContextIdentifierObjectTable implements ObjectTable {
    @Override
    public Writer getObjectWriter(Object o) throws IOException {
        if (o == null) {
            return null;
        }
        // we just care about EJB proxies
        if (!EJBClient.isEJBProxy(o)) {
            return null;
        }
        return EJBClientContextIdentifierWriter.INSTANCE;
    }

    @Override
    public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        return unmarshaller.readObject();
    }

    /**
     * A {@link Writer} which writes out a {@link SerializableEJBProxy} for a
     * EJB proxy
     */
    private static class EJBClientContextIdentifierWriter implements Writer {

        private static final EJBClientContextIdentifierWriter INSTANCE = new EJBClientContextIdentifierWriter();

        @Override
        public void writeObject(Marshaller marshaller, Object o) throws IOException {
            marshaller.writeObject(new SerializableEJBProxy(o));
        }
    }
}
