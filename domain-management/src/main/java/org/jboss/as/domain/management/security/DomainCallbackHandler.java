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

package org.jboss.as.domain.management.security;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

/**
 * An extension of CallbackHandler to allow the supported callbacks to be identified.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface DomainCallbackHandler extends CallbackHandler {

    // TODO - Switch to collections to clean up how these are checked and to introduce safety to prevent the 'set' from being modified.
    Class<Callback>[] getSupportedCallbacks();

    /**
     * Is this DomainCallbackHanler ready for handling remote requests.
     *
     * To be used by the HTTP interface to display an error if the administrator
     * has not completed the set-up of their AS installation.
     *
     * @return indication of if this is ready for remote requests.
     */
    boolean isReady();

}
