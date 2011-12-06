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

package org.jboss.as.remoting;

import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.xnio.OptionMap;
import org.xnio.ssl.XnioSsl;

/**
 * Securing the Remoting connection requires three items, the OptionMap for configuration,
 * the ServerAuthenticationProvider to provide CallbackHandler instances and the XnioSsl instance
 * to wrap the SSLContext.
 *
 * This interface defines how a Provider will make these three items available.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
interface RemotingSecurityProvider {

    /**
     * Obtain the OptionMap containing the configuration for this security
     * provider.
     *
     * @return the generated OptionMap.
     */
    OptionMap getOptionMap();

    /**
     * Obtain the ServerAuthenticationProvider to be used during authentication
     * to obtain mechanism specific CallbackHanler instances.
     *
     * @return the ServerAuthenticationProvier.
     */
    ServerAuthenticationProvider getServerAuthenticationProvider();

    /**
     * Obtain a pre-configured XnioSsl instance to be used when SSL is enabled
     * for the connection.
     *
     * @return the XnioSsl instance.
     */
    XnioSsl getXnioSsl();

}
