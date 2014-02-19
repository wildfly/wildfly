/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.apache.directory.server.ldap.handlers.ssl;

import java.security.SecureRandom;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.filter.ssl.SslFilter;
import org.jboss.as.test.manualmode.security.TrustAndStoreTrustManager;

/**
 * Re-implementation of LdapsInitializer from ApacheDS project, for testing purposes. This version ask for client authentication
 * during SSL handshake and as a TrustManager uses {@link TrustAndStoreTrustManager} instance.
 *
 * @author Josef Cacek
 */
//todo this class needs to go currently it is only here to override the orginal class that is part of apacheds and it only add TrustAndStoreTrustManager
public class LdapsInitializer {

    public static IoFilterChainBuilder init(LdapServer server) throws LdapException {
        SSLContext sslCtx;
        try {
            // Initialize the SSLContext to work with our key managers.
            sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(server.getKeyManagerFactory().getKeyManagers(), new TrustManager[]
                    {new TrustAndStoreTrustManager()}, new SecureRandom());
        } catch (Exception e) {
            throw new LdapException(I18n.err(I18n.ERR_683), e);
        }

        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        SslFilter sslFilter = new SslFilter(sslCtx);

        List<String> cipherSuites = server.getEnabledCipherSuites();
        if ((cipherSuites != null) && !cipherSuites.isEmpty()) {
            sslFilter.setEnabledCipherSuites(cipherSuites.toArray(new String[cipherSuites.size()]));
        }
        sslFilter.setWantClientAuth(true);
        chain.addLast("sslFilter", sslFilter);
        return chain;
    }
}