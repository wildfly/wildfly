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
package org.jboss.as.domain.management;

import javax.net.ssl.SSLContext;

import org.jboss.as.domain.management.security.DomainCallbackHandler;

/**
 * Interface to the security realm.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface SecurityRealm {

    /**
     * @return The name of this SecurityRealm
     */
    String getName();

    /**
     * @return The CallbackHandler for the realm
     */
    DomainCallbackHandler getCallbackHandler();

    /**
     * Used to obtain the SSLContext as configured for this security realm.
     *
     * @return the SSLContext server identity for this realm.
     * @throws IllegalStateException - If no SSL server-identity has been defined.
     */
    SSLContext getSSLContext();

    /**
     * Identify if a trust store has been configured for authentication, if defined
     * it means CLIENT-CERT type authentication can occur.
     *
     * @return true if a trust store has been configured for authentication.
     */
    boolean hasTrustStore();

    /**
     * @return A CallbackHandlerFactory for a pre-configured secret.
     */
    CallbackHandlerFactory getSecretCallbackHandlerFactory();

}
