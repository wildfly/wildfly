/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;


/**
 * The context for registering a new extension.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author David Bosschaert
 */
public interface ExtensionContext {

    /**
     * Register a new subsystem type.  The returned registration object should be used
     * to configure XML parsers, operation handlers, and other subsystem-specific constructs
     * for the new subsystem.  If the subsystem registration is deemed invalid by the time the
     * extension registration is complete, the subsystem registration will be ignored, and an
     * error message will be logged.
     * <p>
     * The new subsystem registration <em>should</em> register a handler and description for the
     * {@code add} operation at its root address.  The new subsystem registration <em>may</em> register a
     * {@code remove} operation at its root address.  If either of these operations are not registered, a
     * simple generic version of the missing operation will be produced.
     *
     * @param name the name of the subsystem
     * @throws IllegalArgumentException if the subsystem name has already been registered
     */
    SubsystemRegistration registerSubsystem(String name) throws IllegalArgumentException;

    ExtensionContext createWrapper(String moduleName);

    void cleanup(String moduleName);
    /**
     * Provide the current Process Type.
     * @return The current Process Type.
     */
    ProcessType getProcessType();

}
