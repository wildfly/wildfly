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
package org.jboss.as.test.integration.security.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.mina.util.AvailablePortFinder;

/**
 * Annotation processor for creating Kerberos servers - based on original implementation in
 * {@link org.apache.directory.server.factory.ServerAnnotationProcessor}. This implementation only adds a workaround for
 * https://issues.apache.org/jira/browse/DIRKRB-85<br/>
 * Use this class together with {@link ExtCreateKdcServer} annotation.
 * 
 * @author Josef Cacek
 * @see ExtCreateKdcServer
 */
public class KDCServerAnnotationProcessor {

    // Public methods --------------------------------------------------------

    /**
     * Creates and starts KdcServer based on configuration from {@link ExtCreateKdcServer} annotation.
     * 
     * @param directoryService
     * @param startPort start port number used for searching free ports in case the transport has no port number preconfigured.
     * @return
     * @throws Exception
     */
    public static KdcServer getKdcServer(DirectoryService directoryService, int startPort) throws Exception {
        ExtCreateKdcServer createKdcServer = getAnnotation(ExtCreateKdcServer.class);
        return createKdcServer(createKdcServer, directoryService, startPort);
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates and starts {@link KdcServer} instance based on given configuration.
     * 
     * @param createKdcServer
     * @param directoryService
     * @param startPort
     * @return
     */
    private static KdcServer createKdcServer(ExtCreateKdcServer createKdcServer, DirectoryService directoryService,
            int startPort) {
        if (createKdcServer == null) {
            return null;
        }
        KdcServer kdcServer = new KdcServer();
        kdcServer.setServiceName(createKdcServer.name());
        kdcServer.setKdcPrincipal(createKdcServer.kdcPrincipal());
        kdcServer.setPrimaryRealm(createKdcServer.primaryRealm());
        kdcServer.setMaximumTicketLifetime(createKdcServer.maxTicketLifetime());
        kdcServer.setMaximumRenewableLifetime(createKdcServer.maxRenewableLifetime());
        kdcServer.setSearchBaseDn(createKdcServer.searchBaseDn());
        kdcServer.setPaEncTimestampRequired(false);

        CreateTransport[] transportBuilders = createKdcServer.transports();

        if (transportBuilders == null) {
            // create only UDP transport if none specified
            UdpTransport defaultTransport = new UdpTransport(AvailablePortFinder.getNextAvailable(startPort));
            kdcServer.addTransports(defaultTransport);
        } else if (transportBuilders.length > 0) {
            for (CreateTransport transportBuilder : transportBuilders) {
                String protocol = transportBuilder.protocol();
                int port = transportBuilder.port();
                int nbThreads = transportBuilder.nbThreads();
                int backlog = transportBuilder.backlog();
                String address = transportBuilder.address();

                if (port == -1) {
                    port = AvailablePortFinder.getNextAvailable(startPort);
                    startPort = port + 1;
                }

                if (protocol.equalsIgnoreCase("TCP")) {
                    Transport tcp = new TcpTransport(address, port, nbThreads, backlog);
                    kdcServer.addTransports(tcp);
                } else if (protocol.equalsIgnoreCase("UDP")) {
                    UdpTransport udp = new UdpTransport(address, port);
                    kdcServer.addTransports(udp);
                } else {
                    throw new IllegalArgumentException(I18n.err(I18n.ERR_689, protocol));
                }
            }
        }

        kdcServer.setDirectoryService(directoryService);

        // Launch the server
        try {
            kdcServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return kdcServer;
    }

    /**
     * Retrieves an annotation of given type from a calling method or a class in which is the method located.
     * 
     * @param annotationClass
     * @return
     * @throws Exception
     */
    private static <T extends Annotation> T getAnnotation(Class<T> annotationClass) throws Exception {
        // Get the caller by inspecting the stackTrace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // In Java5 the 0th stacktrace element is: java.lang.Thread.dumpThreads(Native Method)
        int index = stackTrace[0].getMethodName().equals("dumpThreads") ? 4 : 3;

        // Get the enclosing class
        Class<?> classCaller = Class.forName(stackTrace[index].getClassName());

        // Get the current method
        String methodCaller = stackTrace[index].getMethodName();

        // Check if we have any annotation associated with the method
        Method[] methods = classCaller.getMethods();

        for (Method method : methods) {
            if (methodCaller.equals(method.getName())) {
                T annotation = method.getAnnotation(annotationClass);

                if (annotation != null) {
                    return annotation;
                }
            }
        }

        // No : look at the class level
        return classCaller.getAnnotation(annotationClass);
    }

}
