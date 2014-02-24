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
package org.jboss.as.controller.audit;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.jboss.as.controller.interfaces.InetAddressUtil;

import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.handlers.SyslogHandler;
import org.jboss.logmanager.handlers.SyslogHandler.Protocol;
import org.jboss.logmanager.handlers.SyslogHandler.SyslogType;
import org.jboss.logmanager.handlers.TcpOutputStream;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SyslogAuditLogHandler extends AuditLogHandler {

    private final PathManagerService pathManager;

    private volatile SyslogHandler handler;
    private volatile String appName;
    private volatile String hostName;
    private volatile SyslogType syslogType = SyslogType.RFC5424;
    private volatile boolean truncate;
    private volatile int maxLength;
    private volatile InetAddress syslogServerAddress;
    private volatile int port = 514;
    private volatile Transport transport = Transport.UDP;
    private volatile MessageTransfer messageTransfer = MessageTransfer.NON_TRANSPARENT_FRAMING;
    private volatile Facility facility;
    private volatile String tlsTrustStorePath;
    private volatile String tlsTrustStoreRelativeTo;
    private volatile String tlsTrustStorePassword;
    private volatile String tlsClientCertStorePath;
    private volatile String tlsClientCertStoreRelativeTo;
    private volatile String tlsClientCertStorePassword;
    private volatile String tlsClientCertStoreKeyPassword;

    public SyslogAuditLogHandler(String name, String formatterName, int maxFailureCount, PathManagerService pathManager) {
        super(name, formatterName, maxFailureCount);
        this.pathManager = pathManager;
    }

    public void setHostName(String hostName) {
        assert hostName != null;
        this.hostName = hostName;
    }

    public void setAppName(String appName) {
        assert appName != null;
        this.appName = appName;
        //This gets updated immediately
        if (handler != null) {
            handler.setAppName(appName);
        }
    }

    public void setFacility(Facility facility) {
        assert facility != null;
        this.facility = facility;
        //This gets updated immediately
        if (handler != null) {
            handler.setFacility(facility.convert());
        }
    }

    public void setSyslogType(SyslogType syslogType) {
        assert syslogType != null;
        this.syslogType = syslogType;
    }

    public void setTruncate(boolean truncate) {
        this.truncate = truncate;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public void setMessageTransfer(MessageTransfer messageTransfer) {
        assert messageTransfer != null;
        this.messageTransfer = messageTransfer;
    }

    public void setSyslogServerAddress(InetAddress syslogServerAddress) {
        assert syslogServerAddress != null;
        this.syslogServerAddress = syslogServerAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTransport(Transport transport) {
        assert transport != null;
        this.transport = transport;
    }

    public void setTlsTrustStorePath(String tlsTrustStorePath) {
        this.tlsTrustStorePath = tlsTrustStorePath;
    }

    public void setTlsTrustStoreRelativeTo(String tlsTrustStoreRelativeTo) {
        this.tlsTrustStoreRelativeTo = tlsTrustStoreRelativeTo;
    }

    public void setTlsTruststorePassword(String tlsTrustStorePassword) {
        this.tlsTrustStorePassword = tlsTrustStorePassword;
    }

    public void setTlsClientCertStorePath(String tlsClientCertStorePath) {
        this.tlsClientCertStorePath = tlsClientCertStorePath;
    }

    public void setTlsClientCertStoreRelativeTo(String tlsClientCertStoreRelativeTo) {
        this.tlsClientCertStoreRelativeTo = tlsClientCertStoreRelativeTo;
    }

    public void setTlsClientCertStorePassword(String tlsClientCertStorePassword) {
        this.tlsClientCertStorePassword = tlsClientCertStorePassword;
    }

    public void setTlsClientCertStoreKeyPassword(String tlsClientCertStoreKeyPassword) {
        this.tlsClientCertStoreKeyPassword = tlsClientCertStoreKeyPassword;
    }


    @Override
    void initialize() {
        try {
            if (handler != null) {
                return;
            }
            final Protocol protocol;
            switch (transport) {
            case UDP:
                protocol = Protocol.UDP;
                break;
            case TCP:
                protocol = Protocol.TCP;
                break;
            case TLS:
                protocol = Protocol.SSL_TCP;
                break;
            default:
                //i18n not needed, user code will not end up here
                throw new IllegalStateException("Unknown protocol");
            }
            handler = new SyslogHandler(syslogServerAddress, port, facility.convert(), syslogType, protocol, hostName == null ? InetAddressUtil.getLocalHostName() : hostName);
            handler.setEscapeEnabled(false); //Escaping is handled by the formatter
            handler.setAppName(appName);
            handler.setTruncate(truncate);
            if (maxLength != 0) {
                handler.setMaxLength(maxLength);
            }

            //Common for all protocols
            handler.setSyslogType(syslogType);

            if (transport != Transport.UDP){
                if (messageTransfer == MessageTransfer.NON_TRANSPARENT_FRAMING) {
                    handler.setUseCountingFraming(false);
                    handler.setMessageDelimiter("\n");
                    handler.setUseMessageDelimiter(true);
                } else {
                    handler.setUseCountingFraming(true);
                    handler.setMessageDelimiter(null);
                    handler.setUseMessageDelimiter(false);
                }

                if (transport == Transport.TLS && (tlsClientCertStorePath != null || tlsTrustStorePath != null)){
                    final SSLContext context = SSLContext.getInstance("TLS");
                    KeyManager[] keyManagers = null;
                    if (tlsClientCertStorePath != null){
                        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        final FileInputStream in = new FileInputStream(pathManager.resolveRelativePathEntry(tlsClientCertStorePath, tlsClientCertStoreRelativeTo));
                        try {
                            final KeyStore ks = KeyStore.getInstance("JKS");
                            ks.load(in, tlsClientCertStorePassword.toCharArray());
                            kmf.init(ks, tlsClientCertStoreKeyPassword != null ? tlsClientCertStoreKeyPassword.toCharArray() : tlsClientCertStorePassword.toCharArray());
                            keyManagers = kmf.getKeyManagers();
                        } finally {
                            IoUtils.safeClose(in);
                        }
                    }
                    TrustManager[] trustManagers = null;
                    if (tlsTrustStorePath != null){
                        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                        final FileInputStream in = new FileInputStream(pathManager.resolveRelativePathEntry(tlsTrustStorePath, tlsTrustStoreRelativeTo));
                        try {
                            final KeyStore ks = KeyStore.getInstance("JKS");
                            ks.load(in, tlsTrustStorePassword.toCharArray());
                            tmf.init(ks);
                            trustManagers = tmf.getTrustManagers();
                        } finally {
                            IoUtils.safeClose(in);
                        }

                    }
                    context.init(keyManagers, trustManagers, null);
                    handler.setOutputStream(new SSLContextOutputStream(context, syslogServerAddress, port));
                } else {
                    handler.setProtocol(transport == Transport.TCP ? Protocol.TCP : Protocol.SSL_TCP);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void stop() {
        SyslogHandler handler = this.handler;
        this.handler = null;
        if (handler != null) {
            handler.close();
        }

    }

    @Override
    void writeLogItem(String formattedItem) throws IOException {
        handler.publish(new ExtLogRecord(Level.WARN, formattedItem, SyslogAuditLogHandler.class.getName()));
    }

    @Override
    boolean isDifferent(AuditLogHandler other){
        if (other instanceof SyslogAuditLogHandler == false){
            return true;
        }
        SyslogAuditLogHandler otherHandler = (SyslogAuditLogHandler)other;
        if (!name.equals(otherHandler.name)){
            return true;
        }
        if (!getFormatterName().equals(otherHandler.getFormatterName())) {
            return true;
        }
        if (!hostName.equals(otherHandler.hostName)){
            return true;
        }
        if (!syslogType.equals(otherHandler.syslogType)){
            return true;
        }
        if (!truncate == otherHandler.truncate) {
            return true;
        }
        if (maxLength != otherHandler.maxLength) {
            return true;
        }
        if (!syslogServerAddress.equals(otherHandler.syslogServerAddress)){
            return true;
        }
        if (port != otherHandler.port){
            return true;
        }
        if (!transport.equals(otherHandler.transport)){
            return true;
        }
        //These may or not be null depending on the transport
        if (!compare(messageTransfer, otherHandler.messageTransfer)){
            return true;
        }
        if (!compare(tlsTrustStorePath, otherHandler.tlsTrustStorePath)){
            return true;
        }
        if (!compare(tlsTrustStoreRelativeTo, otherHandler.tlsTrustStoreRelativeTo)){
            return true;
        }
        if (!compare(tlsTrustStorePassword, otherHandler.tlsTrustStorePassword)){
            return true;
        }
        if (!compare(tlsClientCertStorePath, otherHandler.tlsClientCertStorePath)){
            return true;
        }
        if (!compare(tlsClientCertStoreRelativeTo, otherHandler.tlsClientCertStoreRelativeTo)){
            return true;
        }
        if (!compare(tlsClientCertStorePassword, otherHandler.tlsClientCertStorePassword)){
            return true;
        }
        if (!compare(tlsClientCertStoreKeyPassword, otherHandler.tlsClientCertStoreKeyPassword)){
            return true;
        }
        return false;
    }

    private boolean compare(Object one, Object two){
        if (one == null && two == null){
            return true;
        }
        if (one == null && two != null){
            return false;
        }
        if (one != null && two == null){
            return false;
        }
        return one.equals(two);
    }

    public enum Transport {
        UDP,
        TCP,
        TLS
    }

    public enum MessageTransfer {
        OCTET_COUNTING,
        NON_TRANSPARENT_FRAMING;
    }

    /**
     * Facility as defined by RFC-5424 (<a href="http://tools.ietf.org/html/rfc5424">http://tools.ietf.org/html/rfc5424</a>)
     * and RFC-3164 (<a href="http://tools.ietf.org/html/rfc3164">http://tools.ietf.org/html/rfc3164</a>).
     */
    public static enum Facility {
        KERNEL(SyslogHandler.Facility.KERNEL),
        USER_LEVEL(SyslogHandler.Facility.USER_LEVEL),
        MAIL_SYSTEM(SyslogHandler.Facility.MAIL_SYSTEM),
        SYSTEM_DAEMONS(SyslogHandler.Facility.SYSTEM_DAEMONS),
        SECURITY(SyslogHandler.Facility.SECURITY),
        SYSLOGD(SyslogHandler.Facility.SYSLOGD),
        LINE_PRINTER(SyslogHandler.Facility.LINE_PRINTER),
        NETWORK_NEWS(SyslogHandler.Facility.NETWORK_NEWS),
        UUCP(SyslogHandler.Facility.UUCP),
        CLOCK_DAEMON(SyslogHandler.Facility.CLOCK_DAEMON),
        SECURITY2(SyslogHandler.Facility.SECURITY2),
        FTP_DAEMON(SyslogHandler.Facility.FTP_DAEMON),
        NTP(SyslogHandler.Facility.NTP),
        LOG_AUDIT(SyslogHandler.Facility.LOG_AUDIT),
        LOG_ALERT(SyslogHandler.Facility.LOG_ALERT),
        CLOCK_DAEMON2(SyslogHandler.Facility.CLOCK_DAEMON2),
        LOCAL_USE_0(SyslogHandler.Facility.LOCAL_USE_0),
        LOCAL_USE_1(SyslogHandler.Facility.LOCAL_USE_1),
        LOCAL_USE_2(SyslogHandler.Facility.LOCAL_USE_2),
        LOCAL_USE_3(SyslogHandler.Facility.LOCAL_USE_3),
        LOCAL_USE_4(SyslogHandler.Facility.LOCAL_USE_4),
        LOCAL_USE_5(SyslogHandler.Facility.LOCAL_USE_5),
        LOCAL_USE_6(SyslogHandler.Facility.LOCAL_USE_6),
        LOCAL_USE_7(SyslogHandler.Facility.LOCAL_USE_7);

        private final SyslogHandler.Facility realFacility;

        private Facility(SyslogHandler.Facility realFacility) {
            this.realFacility = realFacility;
        }

        public SyslogHandler.Facility convert(){
            return realFacility;
        }
    }


    private static class SSLContextOutputStream extends TcpOutputStream {
        protected SSLContextOutputStream(SSLContext sslContext, InetAddress host, int port) throws IOException {
            super(sslContext.getSocketFactory().createSocket(host, port));
        }
    }

}
