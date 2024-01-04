/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
