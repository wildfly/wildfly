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

package org.jboss.as.model;

import java.util.Collections;
import java.util.List;

/**
 * An update which changes the name property on a host element.
 *
 * @author Brian Stansberry
 */
public final class HostJvmUpdate<R> extends AbstractHostModelUpdate<Void> {
    private static final long serialVersionUID = 6075488950873140885L;


    public static <T> HostJvmUpdate<T> create(final String jvmName, final AbstractModelUpdate<JvmElement, T> jvmUpdate) {
        return new HostJvmUpdate<T>(jvmName, jvmUpdate);
    }


    private final String jvmName;
    private final AbstractModelUpdate<JvmElement, R> jvmUpdate;

    /**
     * Construct a new instance.
     *
     * @param name the name of the host
     */
    public HostJvmUpdate(final String jvmName, final AbstractModelUpdate<JvmElement, R> jvmUpdate) {
        if (jvmName == null)
            throw new IllegalArgumentException("jvmName is null");
        if (jvmUpdate == null)
            throw new IllegalArgumentException("jvmUpdate is null");
        this.jvmName = jvmName;
        this.jvmUpdate = jvmUpdate;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(final HostModel element) throws UpdateFailedException {
        JvmElement jvm = element.getJvm(jvmName);
        if (jvm == null) {
            throw new UpdateFailedException("Host has no JVM '"+ jvmName +"'configured");
        }
        jvmUpdate.applyUpdate(jvm);
    }

    /** {@inheritDoc} */
    @Override
    public HostJvmUpdate<?> getCompensatingUpdate(final HostModel original) {
        JvmElement jvm = original.getJvm(jvmName);
        if (jvm == null) {
            return null;
        }
        return HostJvmUpdate.create(jvmName, jvmUpdate.getCompensatingUpdate(jvm));
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        // JvmElement changes do not affect running servers; they are picked up by
        // HostController when it launches servers
        return null;
    }

    @Override
    public List<String> getAffectedServers(HostModel hostModel) {
        return Collections.emptyList();
    }
}
