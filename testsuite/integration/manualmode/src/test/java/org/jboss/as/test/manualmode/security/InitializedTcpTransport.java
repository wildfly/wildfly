package org.jboss.as.test.manualmode.security;

import java.util.List;

import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.mina.transport.socket.SocketAcceptor;

/**
 * A wrapper around {@link TcpTransport} that delegates all its methods to the {@link #delegate} except for ignoring
 * {@link #init()} as {@link #delegate} is assumed to have been initialized already.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class InitializedTcpTransport extends TcpTransport {
    private final TcpTransport delegate;

    public InitializedTcpTransport(TcpTransport delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }

    @Override
    public void setPort(int port) {
        delegate.setPort(port);
    }

    @Override
    public String getAddress() {
        return delegate.getAddress();
    }

    @Override
    public void setAddress(String address) {
        delegate.setAddress(address);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public void init() {
    }

    @Override
    public int getNbThreads() {
        return delegate.getNbThreads();
    }

    @Override
    public void setNbThreads(int nbThreads) {
        delegate.setNbThreads(nbThreads);
    }

    @Override
    public int getBackLog() {
        return delegate.getBackLog();
    }

    @Override
    public void setBackLog(int backLog) {
        delegate.setBackLog(backLog);
    }

    @Override
    public void setEnableSSL(boolean sslEnabled) {
        delegate.setEnableSSL(sslEnabled);
    }

    @Override
    public void enableSSL(boolean sslEnabled) {
        delegate.enableSSL(sslEnabled);
    }

    @Override
    public SocketAcceptor getAcceptor() {
        return delegate.getAcceptor();
    }

    @Override
    public boolean isSSLEnabled() {
        return delegate.isSSLEnabled();
    }

    @Override
    public boolean getEnableSSL() {
        return delegate.getEnableSSL();
    }

    @Override
    public void setNeedClientAuth(boolean needClientAuth) {
        delegate.setNeedClientAuth(needClientAuth);
    }

    @Override
    public boolean isNeedClientAuth() {
        return delegate.isNeedClientAuth();
    }

    @Override
    public void setWantClientAuth(boolean wantClientAuth) {
        delegate.setWantClientAuth(wantClientAuth);
    }

    @Override
    public boolean isWantClientAuth() {
        return delegate.isWantClientAuth();
    }

    @Override
    public List<String> getEnabledProtocols() {
        return delegate.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(List<String> enabledProtocols) {
        delegate.setEnabledProtocols(enabledProtocols);
    }

    @Override
    public List<String> getCipherSuite() {
        return delegate.getCipherSuite();
    }

    @Override
    public void setEnabledCiphers(List<String> cipherSuite) {
        delegate.setEnabledCiphers(cipherSuite);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

}