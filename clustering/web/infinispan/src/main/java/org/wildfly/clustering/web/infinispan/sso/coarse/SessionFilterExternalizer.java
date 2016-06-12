/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.web.infinispan.SessionIdentifierExternalizer;

/**
 * @author Paul Ferraro
 */
public class SessionFilterExternalizer<D> implements Externalizer<SessionFilter<D>> {

    @Override
    public void writeObject(ObjectOutput output, SessionFilter<D> filter) throws IOException {
        SessionIdentifierExternalizer.BASE64.writeObject(output, filter.getSessionId());
    }

    @Override
    public SessionFilter<D> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new SessionFilter<>(SessionIdentifierExternalizer.BASE64.readObject(input));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Class<? extends SessionFilter<D>> getTargetClass() {
        Class targetClass = SessionFilter.class;
        return targetClass;
    }
}
