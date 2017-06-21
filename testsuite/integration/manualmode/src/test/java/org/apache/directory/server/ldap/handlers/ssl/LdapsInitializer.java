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

import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.security.cert.X509Certificate;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.filter.ssl.SslFilter;
import org.jboss.as.test.manualmode.security.SslCertChainRecorder;

/**
 * Re-implementation of LdapsInitializer from ApacheDS project, for testing purposes. This version allows for setting a custom
 * {@link SslCertChainRecorder} via {@link #setAndLockRecorder(SslCertChainRecorder)} to be able to check which certificates
 * were sent with which connections. The {@coden needClientAuth} and {@coden wantClientAuth} settings are taken from the
 * {@link TcpTransport} passed to {@link #init(LdapServer, TcpTransport)}.
 *
 * @author Josef Cacek
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
//todo this class needs to go currently it is only here to override the original class that is part of apacheds and it only add TrustAndStoreTrustManager
public class LdapsInitializer {

    /**
     * A wrapper around a {@link SSLContext} delegate that allows for passing a {@link SslCertChainRecorder} into the
     * {@link SSLEngine} to be able to check which certificates were sent with which connections.
     */
    private static class WrappedSSLContext extends SSLContext {

        private static class WrappedSSLContextSpi extends SSLContextSpi {
            private final SSLContext delegate;
            private final SslCertChainRecorder recorder;

            public WrappedSSLContextSpi(SSLContext delegate, SslCertChainRecorder recorder) {
                super();
                this.delegate = delegate;
                this.recorder = recorder;
            }

            @Override
            protected SSLEngine engineCreateSSLEngine() {
                return new WrappedSSLEngine(delegate.createSSLEngine(), recorder);
            }

            @Override
            protected SSLEngine engineCreateSSLEngine(String host, int port) {
                return new WrappedSSLEngine(delegate.createSSLEngine(host, port), recorder);
            }

            @Override
            protected SSLSessionContext engineGetClientSessionContext() {
                return delegate.getClientSessionContext();
            }

            @Override
            protected SSLSessionContext engineGetServerSessionContext() {
                return delegate.getServerSessionContext();
            }

            @Override
            protected SSLServerSocketFactory engineGetServerSocketFactory() {
                return delegate.getServerSocketFactory();
            }

            @Override
            protected SSLSocketFactory engineGetSocketFactory() {
                return delegate.getSocketFactory();
            }

            @Override
            protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) throws KeyManagementException {
                delegate.init(km, tm, sr);
            }
        }

        private static class WrappedSSLEngine extends SSLEngine {
            private final SSLEngine delegate;
            private final SslCertChainRecorder recorder;

            public WrappedSSLEngine(SSLEngine delegate, SslCertChainRecorder recorder) {
                super();
                this.delegate = delegate;
                this.recorder = recorder;
            }

            public void beginHandshake() throws SSLException {
                delegate.beginHandshake();
            }

            public void closeInbound() throws SSLException {
                delegate.closeInbound();
            }

            public void closeOutbound() {
                delegate.closeOutbound();
            }

            public boolean equals(Object obj) {
                return delegate.equals(obj);
            }

            public Runnable getDelegatedTask() {
                return delegate.getDelegatedTask();
            }

            public boolean getEnableSessionCreation() {
                return delegate.getEnableSessionCreation();
            }

            public String[] getEnabledCipherSuites() {
                return delegate.getEnabledCipherSuites();
            }

            public String[] getEnabledProtocols() {
                return delegate.getEnabledProtocols();
            }

            public SSLSession getHandshakeSession() {
                return delegate.getHandshakeSession();
            }

            public HandshakeStatus getHandshakeStatus() {
                return delegate.getHandshakeStatus();
            }

            public boolean getNeedClientAuth() {
                return delegate.getNeedClientAuth();
            }

            public String getPeerHost() {
                return delegate.getPeerHost();
            }

            public int getPeerPort() {
                return delegate.getPeerPort();
            }

            public SSLParameters getSSLParameters() {
                return delegate.getSSLParameters();
            }

            public SSLSession getSession() {
                return delegate.getSession();
            }

            public String[] getSupportedCipherSuites() {
                return delegate.getSupportedCipherSuites();
            }

            public String[] getSupportedProtocols() {
                return delegate.getSupportedProtocols();
            }

            public boolean getUseClientMode() {
                return delegate.getUseClientMode();
            }

            public boolean getWantClientAuth() {
                return delegate.getWantClientAuth();
            }

            public int hashCode() {
                return delegate.hashCode();
            }

            public boolean isInboundDone() {
                return delegate.isInboundDone();
            }

            public boolean isOutboundDone() {
                return delegate.isOutboundDone();
            }

            public void setEnableSessionCreation(boolean flag) {
                delegate.setEnableSessionCreation(flag);
            }

            public void setEnabledCipherSuites(String[] suites) {
                delegate.setEnabledCipherSuites(suites);
            }

            public void setEnabledProtocols(String[] protocols) {
                delegate.setEnabledProtocols(protocols);
            }

            public void setNeedClientAuth(boolean need) {
                delegate.setNeedClientAuth(need);
            }

            public void setSSLParameters(SSLParameters params) {
                delegate.setSSLParameters(params);
            }

            public void setUseClientMode(boolean mode) {
                delegate.setUseClientMode(mode);
            }

            public void setWantClientAuth(boolean want) {
                delegate.setWantClientAuth(want);
            }

            public String toString() {
                return delegate.toString();
            }

            public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
                SSLEngineResult result = delegate.unwrap(src, dst);
                recordIfNeeded(result);
                return result;
            }

            public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
                SSLEngineResult result = delegate.unwrap(src, dsts, offset, length);
                recordIfNeeded(result);
                return result;
            }

            private void recordIfNeeded(SSLEngineResult result) {
                switch (result.getHandshakeStatus()) {
                case FINISHED:
                    try {
                        recorder.record(delegate.getSession().getPeerCertificateChain());
                    } catch (SSLPeerUnverifiedException e) {
                        recorder.record(new X509Certificate[0]);
                    }
                    break;
                default:
                    break;
                }
            }

            public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts) throws SSLException {
                SSLEngineResult result = delegate.unwrap(src, dsts);
                recordIfNeeded(result);
                return result;
            }

            public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
                SSLEngineResult result = delegate.wrap(src, dst);
                recordIfNeeded(result);
                return result;
            }

            public SSLEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws SSLException {
                SSLEngineResult result = delegate.wrap(srcs, dst);
                recordIfNeeded(result);
                return result;
            }

            public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
                SSLEngineResult result = delegate.wrap(srcs, offset, length, dst);
                recordIfNeeded(result);
                return result;
            }
        }

        protected WrappedSSLContext(SSLContext delegate, SslCertChainRecorder recorder, Provider provider, String protocol) {
            super(new WrappedSSLContextSpi(delegate, recorder), provider, protocol);
        }

    }
    private static SslCertChainRecorder recorder;

    /** A lock that prevents two tests to use this class simultaneously */
    private static final ReentrantLock recorderLock = new ReentrantLock();

    /**
     * Locks {@link #recorderLock} and sets the given {@code recorder}.
     *
     * @param recorder the {@link SslCertChainRecorder} to set
     */
    public static void setAndLockRecorder(SslCertChainRecorder recorder) {
        recorderLock.lock();
        LdapsInitializer.recorder = recorder;
    }

    /**
     * Sets {@link #recorder} to {@code null} and unlocks {@link #recorderLock}. Needs to be called from the same thread
     * as {@link #setAndLockRecorder(SslCertChainRecorder)}.
     */
    public static void unsetAndUnlockRecorder() {
        LdapsInitializer.recorder = null;
        recorderLock.unlock();
    }

    public static IoFilterChainBuilder init(LdapServer server, TcpTransport transport) throws LdapException {

        if (recorder == null) {
            throw new LdapException("You need to set "+ LdapsInitializer.class.getName() +".recorder before starting the LDAP server");
        }

        SSLContext sslCtx;
        try {
            // Initialize the SSLContext to work with our key managers.
            final SSLContext delegateCtx = SSLContext.getInstance("TLS");
            sslCtx = new WrappedSSLContext(delegateCtx, recorder, delegateCtx.getProvider(), delegateCtx.getProtocol());
            sslCtx.init(server.getKeyManagerFactory().getKeyManagers(), new TrustManager[]
                    {new NoVerificationTrustManager()}, new SecureRandom());
        } catch (Exception e) {
            throw new LdapException(I18n.err(I18n.ERR_683), e);
        }

        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        SslFilter sslFilter = new SslFilter(sslCtx);

        List<String> cipherSuites = transport.getCipherSuite();
        if ((cipherSuites != null) && !cipherSuites.isEmpty()) {
            sslFilter.setEnabledCipherSuites(cipherSuites.toArray(new String[cipherSuites.size()]));
        }

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
