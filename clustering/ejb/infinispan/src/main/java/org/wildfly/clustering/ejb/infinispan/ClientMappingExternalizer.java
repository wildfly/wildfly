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
package org.wildfly.clustering.ejb.infinispan;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;

import org.jboss.as.network.ClientMapping;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class ClientMappingExternalizer implements Externalizer<ClientMapping> {
    private static final Externalizer<InetAddress> ADDRESS_EXTERNALIZER = DefaultExternalizer.INET_ADDRESS.cast(InetAddress.class);

    @Override
    public void writeObject(ObjectOutput output, ClientMapping mapping) throws IOException {
        ADDRESS_EXTERNALIZER.writeObject(output, mapping.getSourceNetworkAddress());
        IndexSerializer.UNSIGNED_BYTE.writeInt(output, mapping.getSourceNetworkMaskBits());
        output.writeUTF(mapping.getDestinationAddress());
        IndexSerializer.UNSIGNED_SHORT.writeInt(output, mapping.getDestinationPort());
    }

    @Override
    public ClientMapping readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        InetAddress sourceAddress = ADDRESS_EXTERNALIZER.readObject(input);
        int sourceNetworkMaskBits = IndexSerializer.UNSIGNED_BYTE.readInt(input);
        String destAddress = input.readUTF();
        int destPort = IndexSerializer.UNSIGNED_SHORT.readInt(input);
        return new ClientMapping(sourceAddress, sourceNetworkMaskBits, destAddress, destPort);
    }

    @Override
    public Class<ClientMapping> getTargetClass() {
        return ClientMapping.class;
    }
}
