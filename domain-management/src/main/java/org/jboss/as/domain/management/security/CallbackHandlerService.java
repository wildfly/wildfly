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

package org.jboss.as.domain.management.security;

import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.domain.management.AuthenticationMechanism;

/**
 * The interface to be implemented by all services supplying callback handlers.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface CallbackHandlerService {

    /**
     * @return The preferred authentication mechanism of the CBH.
     */
    AuthenticationMechanism getPreferredMechanism();

    /**
     * @return A set of additional mechanisms that can be handled by CallbackHandlers supplied by this service.
     */
    Set<AuthenticationMechanism> getSupplementaryMechanisms();

    /**
     * @return The transport independent config options for the CallbackHandler supplied by this service.
     */
    Map<String, String> getConfigurationOptions();

    /**
     * Is this CallbackHandler ready for handling remote requests.
     *
     * To be used by the HTTP interface to display an error if the administrator has not completed the set-up of their AS
     * installation.
     *
     * @return indication of if this is ready for remote requests.
     */
    boolean isReady();

    /**
     * Obtain a CallbackHandler instance for use during authentication.
     *
     * The service can decide if it will return a single shared CallbackHandler or a new one for each call to this method.
     *
     * @return A CallbackHandler instance.
     */
    CallbackHandler getCallbackHandler();

}
