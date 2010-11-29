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

/**
 * Applies a JVM modification to a {@link JvmElement} nested in a {@link ServerGroupElement}.
 *
 * @author Brian Stansberry
 */
public class ServerGroupJvmUpdate<R> extends AbstractModelUpdate<ServerGroupElement, R> {

    private static final long serialVersionUID = 6228523925797316634L;

    private final AbstractModelUpdate<JvmElement, R> jvmUpdate;

    public static <T> ServerGroupJvmUpdate<T> create(final AbstractModelUpdate<JvmElement, T> jvmUpdate) {
        return new ServerGroupJvmUpdate<T>(jvmUpdate);
    }

    public ServerGroupJvmUpdate(final AbstractModelUpdate<JvmElement, R> jvmUpdate) {
        if (jvmUpdate == null)
            throw new IllegalArgumentException("jvmUpdate is null");
        this.jvmUpdate = jvmUpdate;
    }

    @Override
    public ServerGroupJvmUpdate<?> getCompensatingUpdate(ServerGroupElement original) {
        JvmElement jvm = original.getJvm();
        return jvm == null ? null : create(jvmUpdate.getCompensatingUpdate(jvm));
    }

    @Override
    protected AbstractServerModelUpdate<R> getServerModelUpdate() {
        // JvmElement changes do not affect running servers; they are picked up by
        // HostController when it launches servers
        return null;
    }

    @Override
    protected void applyUpdate(ServerGroupElement element) throws UpdateFailedException {
        JvmElement jvm = element.getJvm();
        if (jvm == null) {
            throw new UpdateFailedException("Server group " + element.getName() + " has no JVM configured");
        }
        jvmUpdate.applyUpdate(jvm);
    }

    @Override
    public Class<ServerGroupElement> getModelElementType() {
        return ServerGroupElement.class;
    }

}
