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
package org.wildfly.clustering.web.infinispan.session;

import java.util.Map;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;

/**
 * Factory for creating sessions.  This represents the cache mapping strategy for sessions.
 * @author Paul Ferraro
 */
public interface SessionFactory<MV, AV, L> extends Creator<String, Map.Entry<MV, AV>, Void>, Locator<String, Map.Entry<MV, AV>>, Remover<String> {
    SessionMetaDataFactory<MV, L> getMetaDataFactory();
    SessionAttributesFactory<AV> getAttributesFactory();

    Session<L> createSession(String id, Map.Entry<MV, AV> entry);

    default ImmutableSession createImmutableSession(String id, Map.Entry<MV, AV> entry) {
        return this.createImmutableSession(id, this.getMetaDataFactory().createImmutableSessionMetaData(id, entry.getKey()), this.getAttributesFactory().createImmutableSessionAttributes(id, entry.getValue()));
    }

    ImmutableSession createImmutableSession(String id, ImmutableSessionMetaData metaData, ImmutableSessionAttributes attributes);
}
