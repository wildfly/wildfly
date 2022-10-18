/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * {@link Recordable} facade to a registry of {@link Recordable} instances.
 * @author Paul Ferraro
 */
public class InactiveSessionMetaDataRecorder implements Registrar<Recordable<ImmutableSessionMetaData>>, Recordable<ImmutableSessionMetaData> {

    private final Collection<Recordable<ImmutableSessionMetaData>> recorders = new CopyOnWriteArrayList<>();

    @Override
    public void record(ImmutableSessionMetaData metaData) {
        for (Recordable<ImmutableSessionMetaData> recorder : this.recorders) {
            recorder.record(metaData);
        }
    }

    @Override
    public void reset() {
        this.recorders.forEach(Recordable::reset);
    }

    @Override
    public Registration register(Recordable<ImmutableSessionMetaData> recorder) {
        this.recorders.add(recorder);
        return () -> this.recorders.remove(recorder);
    }
}
