/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jgroups.UpHandler;
import org.jgroups.blocks.mux.Muxer;

/**
 * Generate mux id per cache.
 * It should be thread safe due to cache lock.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MuxIdGenerator implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<Short> ids = new TreeSet<Short>(); // making sure muxer is free
    private Map<String, Short> map = new HashMap<String, Short>();
    private Map<String, Integer> counter = new HashMap<String, Integer>();

    void increment(Muxer<UpHandler> muxer, final String appId) {
        Short existing = map.get(appId);
        if (existing == null) {
            for (short id = 1; id <= Short.MAX_VALUE; id++) {
                if (muxer.get(id) == null && ids.add(id)) {
                    existing = id;
                    map.put(appId, existing);
                    counter.put(appId, 1);
                    break;
                }
            }
        } else {
            Integer current = counter.get(appId);
            counter.put(appId, current + 1);
        }
    }

    void decrement(final String appId) {
        Short existing = map.get(appId);
        if (existing != null) {
            Integer current = counter.get(appId);
            if (current != null) {
                int x = current - 1;
                if (x == 0) {
                    ids.remove(existing);
                    map.remove(appId);
                    counter.remove(appId);
                } else {
                    counter.put(appId, x);
                }
            }
        }
    }

    public Short getMuxId(final String appId) {
        return map.get(appId);
    }
}
