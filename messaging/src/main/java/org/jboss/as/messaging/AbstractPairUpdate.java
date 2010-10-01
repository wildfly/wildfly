/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import java.util.Map;

import org.hornetq.api.core.Pair;
import org.jboss.as.model.UpdateFailedException;

/**
 * The abstract pair update.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractPairUpdate<A, B> extends AbstractMessagingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 6172053691870351651L;

    /** The pair. */
    private final Pair<A, B> pair;

    public AbstractPairUpdate(final Pair<A, B> pair) {
        super();
        this.pair = pair;
    }

    public AbstractPairUpdate(final Pair<A, B> pair, boolean restart) {
        super(restart);
        this.pair = pair;
    }

    A getKey() {
        return pair.a;
    }

    B getValue() {
        return pair.b;
    }

    void add(Map<A, B> map) throws UpdateFailedException {
        if(map.containsKey(getKey())) {
            throw new UpdateFailedException("duplicate key " + getKey());
        }
        map.put(getKey(), getValue());
    }

    B get(Map<A, B> map) {
        return map.get(getKey());
    }

}
