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
package org.wildfly.clustering.web.infinispan.sso.coarse;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.Set;

import org.jboss.as.clustering.infinispan.io.AbstractSimpleExternalizer;
import org.wildfly.clustering.web.sso.WebApplication;
import org.wildfly.clustering.web.sso.AuthenticationType;

public class CoarseSSOCacheEntryExternalizer extends AbstractSimpleExternalizer<CoarseSSOCacheEntry<?>> {
    private static final long serialVersionUID = 4667240286133879206L;

    public CoarseSSOCacheEntryExternalizer() {
        this(CoarseSSOCacheEntry.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private CoarseSSOCacheEntryExternalizer(Class targetClass) {
        super(targetClass);
    }

    @Override
    public void writeObject(ObjectOutput output, CoarseSSOCacheEntry<?> entry) throws IOException {
        output.writeByte(entry.getAuthenticationType().ordinal());
        output.writeUTF(entry.getUser());
        output.writeUTF(entry.getPassword());
        Set<Map.Entry<WebApplication, String>> sessions = entry.getSessions().entrySet();
        output.writeInt(sessions.size());
        for (Map.Entry<WebApplication, String> session: sessions) {
            WebApplication application = session.getKey();
            output.writeUTF(application.getContext());
            output.writeUTF(application.getHost());
            output.writeUTF(session.getValue());
        }
    }

    @Override
    public CoarseSSOCacheEntry<?> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        CoarseSSOCacheEntry<?> entry = new CoarseSSOCacheEntry<>();
        entry.setAuthenticationType(AuthenticationType.values()[input.readByte()]);
        entry.setUser(input.readUTF());
        entry.setPassword(input.readUTF());
        int size = input.readInt();
        for (int i = 0; i < size; ++i) {
            entry.getSessions().put(new WebApplication(input.readUTF(), input.readUTF()), input.readUTF());
        }
        return entry;
    }
}
