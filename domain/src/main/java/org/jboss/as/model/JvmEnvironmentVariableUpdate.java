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
 * Updates a {@link JvmElement}'s {@link JvmElement#getSystemProperties() system properties}.
 *
 * @author Brian Stansberry
 */
public class JvmEnvironmentVariableUpdate extends AbstractModelUpdate<JvmElement, Void> {

    private static final long serialVersionUID = -3406895728835596414L;

    private final AbstractPropertyUpdate propertyUpdate;

    public JvmEnvironmentVariableUpdate(final AbstractPropertyUpdate propertyUpdate) {
        this.propertyUpdate = propertyUpdate;
    }

    @Override
    public JvmEnvironmentVariableUpdate getCompensatingUpdate(JvmElement original) {
        return new JvmEnvironmentVariableUpdate(propertyUpdate.getCompensatingUpdate(original.getSystemProperties()));
    }

    @Override
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        // JvmElement changes do not affect running servers; they are picked up by
        // HostController when it launches servers
        return null;
    }

    @Override
    protected void applyUpdate(JvmElement element) throws UpdateFailedException {
        propertyUpdate.applyUpdate(element.getEnvironmentVariables());
    }

    @Override
    public Class<JvmElement> getModelElementType() {
        return JvmElement.class;
    }

}
