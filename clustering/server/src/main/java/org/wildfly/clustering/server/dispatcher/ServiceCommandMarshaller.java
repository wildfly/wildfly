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
package org.wildfly.clustering.server.dispatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.MembershipListener;

public class ServiceCommandMarshaller<C> implements CommandMarshaller<C> {
    private final ServiceName service;
    private final MarshallingContext marshallingContext;
    private final Map<ServiceName, Map.Entry<MembershipListener, Object>> services;
    private final int currentVersion;

    public ServiceCommandMarshaller(ServiceName service, C context, MembershipListener listener, Map<ServiceName, Map.Entry<MembershipListener, Object>> services, MarshallingContext marshallingContext, int currentVersion) {
        this.service = service;
        this.services = services;
        this.services.put(this.service, new SimpleImmutableEntry<MembershipListener, Object>(listener, context));
        this.marshallingContext = marshallingContext;
        this.currentVersion = currentVersion;
    }

    @Override
    public <R> byte[] marshal(Command<R, C> command) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            output.write(this.currentVersion);
            try (Marshaller marshaller = this.marshallingContext.createMarshaller(this.currentVersion)) {
                marshaller.start(Marshalling.createByteOutput(output));
                marshaller.writeUTF(this.service.getCanonicalName());
                marshaller.writeObject(command);
                marshaller.flush();
            }
            return output.toByteArray();
        }
    }

    @Override
    public void close() {
        this.services.remove(this.service);
    }
}