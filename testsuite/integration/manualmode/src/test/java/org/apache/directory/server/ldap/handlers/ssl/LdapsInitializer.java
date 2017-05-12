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
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.filter.ssl.SslFilter;

/**
 * Re-implementation of LdapsInitializer from ApacheDS project, for testing purposes. This version allows for setting a custom
 * {@link TrustManager} via {@link #setAndLockTrustManager(TrustManager)}. The {@coden needClientAuth} and
 * {@coden wantClientAuth} settings are taken from the {@link TcpTransport} passed to {@link #init(LdapServer, TcpTransport)}.
 *
 * @author Josef Cacek
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
//todo this class needs to go currently it is only here to override the original class that is part of apacheds and it only add TrustAndStoreTrustManager
public class LdapsInitializer {

    private static TrustManager trustManager;

    /** A lock that prevents two tests to use this class simultaneously */
    private static final ReentrantLock trustManagerLock = new ReentrantLock();

    /**
     * Locks {@link #trustManagerLock} and sets the given {@code trustManager}.
     *
     * @param trustManager the {@link TrustManager} to set
     */
    public static void setAndLockTrustManager(TrustManager trustManager) {
        trustManagerLock.lock();
        LdapsInitializer.trustManager = trustManager;
    }

    /**
     * Sets {@link #trustManager} to {@code null} and unlocks {@link #trustManagerLock}. Needs to be called from the same thread
     * as {@link #setAndLockTrustManager(TrustManager)}.
     */
    public static void unsetAndUnlockTrustManager() {
        LdapsInitializer.trustManager = null;
        trustManagerLock.unlock();
    }

    public static IoFilterChainBuilder init(LdapServer server, TcpTransport transport) throws LdapException {

        if (trustManager == null) {
            throw new LdapException("You need to set "+ LdapsInitializer.class.getName() +".trustManager before starting the LDAP server");
        }

        SSLContext sslCtx;
        try {
            // Initialize the SSLContext to work with our key managers.
            sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(server.getKeyManagerFactory().getKeyManagers(), new TrustManager[]
                    {trustManager}, new SecureRandom());
        } catch (Exception e) {
            throw new LdapException(I18n.err(I18n.ERR_683), e);
        }

        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        SslFilter sslFilter = new SslFilter(sslCtx);

        List<String> cipherSuites = transport.getCipherSuite();
        if ((cipherSuites != null) && !cipherSuites.isEmpty()) {
            sslFilter.setEnabledCipherSuites(cipherSuites.toArray(new String[cipherSuites.size()]));
        }
        sslFilter.setWantClientAuth(true);

        // The protocols
        List<String> enabledProtocols = transport.getEnabledProtocols();

        if ((enabledProtocols != null) && !enabledProtocols.isEmpty()) {
            sslFilter.setEnabledProtocols(enabledProtocols.toArray(new String[enabledProtocols.size()]));
        } else {
            // Be sure we disable SSLV3
            sslFilter.setEnabledProtocols(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" });
        }

        // The remaining SSL parameters
        sslFilter.setNeedClientAuth(transport.isNeedClientAuth());
        sslFilter.setWantClientAuth(transport.isWantClientAuth());

        chain.addLast("sslFilter", sslFilter);
        return chain;
    }
}
