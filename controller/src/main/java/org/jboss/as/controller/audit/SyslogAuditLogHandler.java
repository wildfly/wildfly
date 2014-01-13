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
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

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
    private volatile String appName = "WildFly";
    private volatile String hostName;
    private volatile SyslogType syslogType = SyslogType.RFC5424;
    private volatile boolean truncate;
    private volatile int maxLength;
    private volatile InetAddress syslogServerAddress;
    private volatile int port = 514;
    private volatile Transport transport = Transport.UDP;
    private volatile MessageTransfer messageTransfer = MessageTransfer.NON_TRANSPARENT_FRAMING;
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
        this.hostName = hostName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setSyslogType(SyslogType syslogType) {
        this.syslogType = syslogType;
    }

    public void setTruncate(boolean truncate) {
        this.truncate = truncate;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public void setMessageTransfer(MessageTransfer messageTransfer) {
        this.messageTransfer = messageTransfer;
    }

    public void setSyslogServerAddress(InetAddress syslogServerAddress) {
        this.syslogServerAddress = syslogServerAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTransport(Transport transport) {
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
            handler = new SyslogHandler(syslogServerAddress, port, tempHackFacilityFromProperty(), syslogType, protocol, hostName == null ? InetAddress.getLocalHost().getHostName() : hostName);
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
        if (!appName.equals(otherHandler.appName)){
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

    private static class SSLContextOutputStream extends TcpOutputStream {
        protected SSLContextOutputStream(SSLContext sslContext, InetAddress host, int port) throws IOException {
            super(sslContext.getSocketFactory().createSocket(host, port));
        }
    }

    // Temp hack for syslog
    private SyslogHandler.Facility tempHackFacilityFromProperty() {
        String prop = System.getSecurityManager() == null ? GetPropertyAction.INSTANCE.run() : AccessController.doPrivileged(GetPropertyAction.INSTANCE);
        if (prop != null) {
            SyslogHandler.Facility facility = FACILITIES.get(prop);
            if (facility != null) {
                return facility;
            }
        }
        return SyslogHandler.DEFAULT_FACILITY;
    }

    private static class GetPropertyAction implements PrivilegedAction<String> {
        static final GetPropertyAction INSTANCE = new GetPropertyAction();
        @Override
        public String run() {
            return System.getProperty("org.jboss.TEMP.audit.log.facility");
        }
    }

   //Temp hack just to be able to test
    SyslogHandler.Facility getHandlerFacility(){
        if (handler == null) {
            return null;
        }
        return handler.getFacility();
    }

    //
    private static final Map<String, SyslogHandler.Facility> FACILITIES;
    static {
        Map<String, SyslogHandler.Facility> map = new HashMap<String, SyslogHandler.Facility>();
        map.put("0", SyslogHandler.Facility.KERNEL);
        map.put("1", SyslogHandler.Facility.USER_LEVEL);
        map.put("2", SyslogHandler.Facility.MAIL_SYSTEM);
        map.put("3", SyslogHandler.Facility.SYSTEM_DAEMONS);
        map.put("4", SyslogHandler.Facility.SECURITY);
        map.put("5", SyslogHandler.Facility.SYSLOGD);
        map.put("6", SyslogHandler.Facility.LINE_PRINTER);
        map.put("7", SyslogHandler.Facility.NETWORK_NEWS);
        map.put("8", SyslogHandler.Facility.UUCP);
        map.put("9", SyslogHandler.Facility.CLOCK_DAEMON);
        map.put("10", SyslogHandler.Facility.SECURITY2);
        map.put("11", SyslogHandler.Facility.FTP_DAEMON);
        map.put("12", SyslogHandler.Facility.NTP);
        map.put("13", SyslogHandler.Facility.LOG_AUDIT);
        map.put("14", SyslogHandler.Facility.LOG_ALERT);
        map.put("15", SyslogHandler.Facility.CLOCK_DAEMON2);
        map.put("16", SyslogHandler.Facility.LOCAL_USE_0);
        map.put("17", SyslogHandler.Facility.LOCAL_USE_1);
        map.put("18", SyslogHandler.Facility.LOCAL_USE_2);
        map.put("19", SyslogHandler.Facility.LOCAL_USE_3);
        map.put("20", SyslogHandler.Facility.LOCAL_USE_4);
        map.put("21", SyslogHandler.Facility.LOCAL_USE_5);
        map.put("22", SyslogHandler.Facility.LOCAL_USE_6);
        map.put("23", SyslogHandler.Facility.LOCAL_USE_7);
        FACILITIES = Collections.unmodifiableMap(map);
    }

}
