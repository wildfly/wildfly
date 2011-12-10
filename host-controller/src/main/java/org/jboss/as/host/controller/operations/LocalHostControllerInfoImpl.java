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

/**
 * Default implementation of {@link LocalHostControllerInfo}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LocalHostControllerInfoImpl implements LocalHostControllerInfo {

    private final ControlledProcessState processState;

    private String localHostName;
    private boolean master;
    private String nativeManagementInterface;
    private int nativeManagementPort;

    private String remoteDcHost;
    private int remoteDcPort;
    private String remoteSecurityRealm;
    private String httpManagementInterface;
    private int httpManagementPort;
    private int httpManagementSecurePort;
    private String nativeManagementSecurityRealm;
    private String httpManagementSecurityRealm;

    public LocalHostControllerInfoImpl(final ControlledProcessState processState) {
        this.processState = processState;
    }

    public String getLocalHostName() {
        return localHostName;
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

    public int getRemoteDomainControllertPort() {
        return remoteDcPort;
    }

    public String getRemoteDomainControllerSecurityRealm() {
        return remoteSecurityRealm;
    }

    void setMasterDomainController(boolean master) {
        this.master = master;
    }

    void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
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

    public void setRemoteDomainControllerSecurityRealm(String remoteSecurityRealm) {
        this.remoteSecurityRealm = remoteSecurityRealm;
    }
}
