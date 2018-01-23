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
package org.wildfly.clustering.ejb.infinispan.bean;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

import org.jboss.ejb.client.SessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.infinispan.SessionIDSerializer;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class InfinispanBeanEntryExternalizer implements Externalizer<InfinispanBeanEntry<SessionID>> {

    @Override
    public void writeObject(ObjectOutput output, InfinispanBeanEntry<SessionID> entry) throws IOException {
        output.writeUTF(entry.getBeanName());
        SessionIDSerializer.INSTANCE.write(output, entry.getGroupId());
        Date lastAccessedTime = entry.getLastAccessedTime();
        output.writeLong((lastAccessedTime != null) ? lastAccessedTime.getTime() : 0);
    }

    @Override
    public InfinispanBeanEntry<SessionID> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        InfinispanBeanEntry<SessionID> entry = new InfinispanBeanEntry<>(input.readUTF(), SessionIDSerializer.INSTANCE.read(input));
        long time = input.readLong();
        if (time > 0) {
            entry.setLastAccessedTime(new Date(time));
        }
        return entry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<InfinispanBeanEntry<SessionID>> getTargetClass() {
        return (Class<InfinispanBeanEntry<SessionID>>) (Class<?>) InfinispanBeanEntry.class;
    }
}
