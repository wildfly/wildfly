/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.naming;


/**
 * A {@link ManagedReferenceFactory} which knows the class name of its {@link ManagedReference} object instance. This type of
 * {@link ManagedReferenceFactory} should be used for JNDI bindings, the {@link ServiceBasedNamingStore} relies on it to provide
 * proper support for {@link javax.naming.Context} list operations.
 *
 * @author Eduardo Martins
 *
 */
public interface ContextListManagedReferenceFactory extends ManagedReferenceFactory {

    String DEFAULT_INSTANCE_CLASS_NAME = Object.class.getName();

    /**
     * Retrieves the reference's object instance class name.
     *
     * If it's impossible to obtain such data, the factory should return the static attribute DEFAULT_INSTANCE_CLASS_NAME,
     * exposed by this interface.
     *
     * @return
     */
    String getInstanceClassName();
}
