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
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class ClientMappingExternalizer implements Externalizer<ClientMapping> {

    @Override
    public void writeObject(ObjectOutput output, ClientMapping mapping) throws IOException {
        byte[] address = mapping.getSourceNetworkAddress().getAddress();
        IndexExternalizer.UNSIGNED_BYTE.writeData(output, address.length);
        output.write(address);
        IndexExternalizer.UNSIGNED_BYTE.writeData(output, mapping.getSourceNetworkMaskBits());
        output.writeUTF(mapping.getDestinationAddress());
        IndexExternalizer.UNSIGNED_SHORT.writeData(output, mapping.getDestinationPort());
    }

    @Override
    public ClientMapping readObject(ObjectInput input) throws IOException {
        byte[] sourceAddress = new byte[IndexExternalizer.UNSIGNED_BYTE.readData(input)];
        input.readFully(sourceAddress);
        int sourceNetworkMaskBits = IndexExternalizer.UNSIGNED_BYTE.readData(input);
        String destAddress = input.readUTF();
        int destPort = IndexExternalizer.UNSIGNED_SHORT.readData(input);
        return new ClientMapping(InetAddress.getByAddress(sourceAddress), sourceNetworkMaskBits, destAddress, destPort);
    }

    @Override
    public Class<ClientMapping> getTargetClass() {
        return ClientMapping.class;
    }
}
