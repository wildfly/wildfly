/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.client;

import java.io.IOException;

import org.jboss.ejb.client.EJBClient;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.ObjectTable.Writer;

/**
 * An {@link ObjectTable} for marshalling an EJB proxy.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link EJBProxyResolver}.
 */
@Deprecated
public class EJBProxyObjectTable implements ObjectTable, Writer {

    @Override
    public Writer getObjectWriter(Object o) throws IOException {
        if (o == null) {
            return null;
        }
        // we just care about Jakarta Enterprise Beans proxies
        if (!EJBClient.isEJBProxy(o)) {
            return null;
        }
        return this;
    }

    @Override
    public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        return unmarshaller.readObject();
    }

    @Override
    public void writeObject(Marshaller marshaller, Object object) throws IOException {
        marshaller.writeObject(new SerializableEJBProxy(object));
    }
}
