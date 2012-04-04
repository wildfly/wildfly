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

package org.jboss.as.host.controller.operations;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.HostControllerEnvironment;

/**
 * Default implementation of {@link LocalHostControllerInfo}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LocalHostControllerInfoImpl implements LocalHostControllerInfo {

    private final ControlledProcessState processState;
    private final HostControllerEnvironment hostEnvironment;

    private final String localHostName;
    private boolean master;
    private String nativeManagementInterface;
    private int nativeManagementPort;

    private String remoteDcHost;
    private int remoteDcPort;
    private String remoteDCUser;
    private String remoteSecurityRealm;
    private String httpManagementInterface;
    private int httpManagementPort;
    private int httpManagementSecurePort;
    private String nativeManagementSecurityRealm;
    private String httpManagementSecurityRealm;

    /** Constructor solely for test cases */
    public LocalHostControllerInfoImpl(final ControlledProcessState processState, final String localHostName) {
        this.processState = processState;
        this.hostEnvironment = null;
        this.localHostName = localHostName;
    }

    public LocalHostControllerInfoImpl(final ControlledProcessState processState, final HostControllerEnvironment hostEnvironment) {
        this.processState = processState;
        this.hostEnvironment = hostEnvironment;
        this.localHostName = null;
    }

    public String getLocalHostName() {
        return hostEnvironment == null ? localHostName : hostEnvironment.getHostControllerName();
    }

    @Override
    public ControlledProcessState.State getProcessState() {
        return processState.getState();
    }

    public boolean isMasterDomainController() {
        return master;
    }

    @Override
    public String getNativeManagementInterface() {
        return nativeManagementInterface;
    }

    @Override
    public int getNativeManagementPort() {
        return nativeManagementPort;
    }

    @Override
    public String getNativeManagementSecurityRealm() {
        return nativeManagementSecurityRealm;
    }

    @Override
    public String getHttpManagementInterface() {
        return httpManagementInterface;
    }

    @Override
    public int getHttpManagementPort() {
        return httpManagementPort;
    }

    @Override
    public int getHttpManagementSecurePort() {
        return httpManagementSecurePort;
    }

    @Override
    public String getHttpManagementSecurityRealm() {
        return httpManagementSecurityRealm;
    }

    public String getRemoteDomainControllerHost() {
        return remoteDcHost;
    }

    public int getRemoteDomainControllerPort() {
        return remoteDcPort;
    }

    public String getRemoteDomainControllerUsername() {
        return remoteDCUser;
    }

    public String getRemoteDomainControllerSecurityRealm() {
        return remoteSecurityRealm;
    }

    void setMasterDomainController(boolean master) {
        this.master = master;
    }

    void setNativeManagementInterface(String nativeManagementInterface) {
        this.nativeManagementInterface = nativeManagementInterface;
    }

    void setNativeManagementPort(int nativeManagementPort) {
        this.nativeManagementPort = nativeManagementPort;
    }

    void setNativeManagementSecurityRealm(String nativeManagementSecurityRealm) {
        this.nativeManagementSecurityRealm = nativeManagementSecurityRealm;
    }

    void setHttpManagementInterface(String httpManagementInterface) {
        this.httpManagementInterface = httpManagementInterface;
    }

    void setHttpManagementPort(int httpManagementPort) {
        this.httpManagementPort = httpManagementPort;
    }

    void setHttpManagementSecurePort(int httpManagementSecurePort) {
        this.httpManagementSecurePort = httpManagementSecurePort;
    }

    void setHttpManagementSecurityRealm(String httpManagementSecurityRealm) {
        this.httpManagementSecurityRealm = httpManagementSecurityRealm;
    }

    void setRemoteDomainControllerHost(String host) {
        remoteDcHost = host;
    }

    void setRemoteDomainControllerPort(int port) {
        remoteDcPort = port;
    }

    void setRemoteDomainControllerUsername(String userName) {
        this.remoteDCUser = userName;
    }

    void setRemoteDomainControllerSecurityRealm(String remoteSecurityRealm) {
        this.remoteSecurityRealm = remoteSecurityRealm;
    }
}
