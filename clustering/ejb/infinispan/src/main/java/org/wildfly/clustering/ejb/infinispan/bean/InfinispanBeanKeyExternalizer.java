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

import org.infinispan.commons.marshall.Externalizer;
import org.jboss.as.clustering.infinispan.io.AbstractSimpleExternalizer;
import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.ejb.infinispan.SessionIDExternalizer;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanKeyExternalizer extends AbstractSimpleExternalizer<InfinispanBeanKey<SessionID>> {
    private static final long serialVersionUID = -7421324153578768415L;

    private final Externalizer<SessionID> externalizer = new SessionIDExternalizer();

    public InfinispanBeanKeyExternalizer() {
        this(InfinispanBeanKey.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private InfinispanBeanKeyExternalizer(Class targetClass) {
        super(targetClass);
    }

    @Override
    public void writeObject(ObjectOutput output, InfinispanBeanKey<SessionID> key) throws IOException {
        output.writeUTF(key.getBeanName());
        this.externalizer.writeObject(output, key.getId());
    }

    @Override
    public InfinispanBeanKey<SessionID> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        String beanName = input.readUTF();
        SessionID id = this.externalizer.readObject(input);
        return new InfinispanBeanKey<>(beanName, id);
    }
}
