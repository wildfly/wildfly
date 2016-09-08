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

package org.wildfly.clustering.web.infinispan.session.fine;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache entry containing the names of the attributes of a session.
 * @author Paul Ferraro
 */
public class SessionAttributeNamesEntry {
    private final AtomicInteger sequence;
    private final ConcurrentMap<String, Integer> names;

    public SessionAttributeNamesEntry(AtomicInteger sequence, ConcurrentMap<String, Integer> names) {
        this.sequence = sequence;
        this.names = names;
    }

    public AtomicInteger getSequence() {
        return this.sequence;
    }

    public ConcurrentMap<String, Integer> getNames() {
        return this.names;
    }
}
