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
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ExtensionContext {

    /**
     * Register a new subsystem type.  The returned registration object should be used
     * to configure XML parsers, operation handlers, and other subsystem-specific constructs
     * for the new subsystem.  If the subsystem registration is deemed invalid by the time the
     * extension registration is complete, the subsystem registration will be ignored, and an
     * error message will be logged.
     * <p>
     * The new subsystem registration <em>must</em> register a handler and description for the
     * {@code add} operation at its root address.  The new subsystem registration <em>must</em> register a
     * {@code remove} operation at its root address.
     *
     * @param name the name of the subsystem
     *
     * @return the {@link SubsystemRegistration}
     * @throws IllegalStateException if the subsystem name has already been registered
     *
     * @deprecated use {@link #registerSubsystem(String, int, int)}
     */
    @Deprecated
    SubsystemRegistration registerSubsystem(String name);

    /**
     * Register a new subsystem type.  The returned registration object should be used
     * to configure XML parsers, operation handlers, and other subsystem-specific constructs
     * for the new subsystem.  If the subsystem registration is deemed invalid by the time the
     * extension registration is complete, the subsystem registration will be ignored, and an
     * error message will be logged.
     * <p>
     * The new subsystem registration <em>must</em> register a handler and description for the
     * {@code add} operation at its root address.  The new subsystem registration <em>must</em> register a
     * {@code remove} operation at its root address.
     *
     * @param name the name of the subsystem
     * @param majorVersion the major version of the subsystem's management interface
     * @param minorVersion the minor version of the subsystem's management interface
     *
     * @return the {@link SubsystemRegistration}
     *
     * @throws IllegalStateException if the subsystem name has already been registered
     */
    SubsystemRegistration registerSubsystem(String name, int majorVersion, int minorVersion);

    /**
     * Gets the type of the current process.
     * @return the current process type. Will not be {@code null}
     */
    ProcessType getProcessType();

    /**
     * Gets the current running mode of the process.
     * @return the current running mode. Will not be {@code null}
     */
    RunningMode getRunningMode();

    /**
     * Gets whether it is valid for the extension to register resources, attributes or operations that do not
     * involve the persistent configuration, but rather only involve runtime services. Extensions should use this
     * method before registering such "runtime only" resources, attributes or operations. The specific uses case this
     * method is intended to support is avoiding registering resources, attributes or operations:
     *
     * <ul>
     *     <li>on a Host Controller (which is only concerned with subsystem configuration) and typically doesn't
     *     install runtime services associated with a subsystem</li>
     *     <li>on a server whose running mode is {@link RunningMode#ADMIN_ONLY ADMIN_ONLY}, where again the
     *     runtime services associated with a subsystem typically would not be installed</li>
     * </ul>
     * <p>
     * This method is a shorthand for:
     * <pre>
     *     boolean valid = context.getProcessType().isServer() && context.getRunningMode() != RunningMode.ADMIN_ONLY;
     * </pre>
     * </p>
     *
     * @return whether the current process type is a server and the server running mode is not ADMIN_ONLY
     */
    boolean isRuntimeOnlyRegistrationValid();

}
