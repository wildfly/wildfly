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
package org.wildfly.extension.picketlink.federation.service;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.config.federation.AuthPropertyType;
import org.picketlink.config.federation.KeyProviderType;
import org.picketlink.config.federation.KeyValueType;
import org.wildfly.extension.picketlink.federation.FederationExtension;

import java.util.ArrayList;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class KeyStoreProviderService implements Service<KeyStoreProviderService> {

    private static final String SERVICE_NAME = "KeyStoreProviderService";

    private volatile KeyProviderType keyProviderType;
    private final InjectedValue<FederationService> federationService = new InjectedValue<FederationService>();
    private volatile  String relativeTo;
    private volatile String filePath;
    private final InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();

    public KeyStoreProviderService(KeyProviderType keyProviderType, String filePath, String relativeTo) {
        this.keyProviderType = keyProviderType;
        this.filePath = filePath;
        this.relativeTo = relativeTo;
    }

    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(FederationExtension.SUBSYSTEM_NAME, SERVICE_NAME, alias);
    }

    @Override
    public KeyStoreProviderService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        setKeyProviderType(this.keyProviderType, this.filePath, this.relativeTo);
    }

    @Override
    public void stop(StopContext context) {
        setKeyProviderType(null, null, null);
        context.getController().setMode(ServiceController.Mode.REMOVE);
    }

    public InjectedValue<FederationService> getFederationService() {
        return this.federationService;
    }

    public InjectedValue<PathManager> getPathManager() {
        return this.pathManager;
    }

    private void configureKeyStoreFilePath() {
        if (this.keyProviderType != null) {
            String resolvedPath;

            // if relative path is null, we use the file path only. This is because the file path can point to a resource inside the deployment,
            // loaded from its classpath.
            if (this.relativeTo != null) {
                resolvedPath = getPathManager().getValue().resolveRelativePathEntry(this.filePath, this.relativeTo);
            } else {
                resolvedPath = this.filePath;
            }

            AuthPropertyType keyStoreURL = new AuthPropertyType();

            keyStoreURL.setKey("KeyStoreURL");
            keyStoreURL.setValue(resolvedPath);

            this.keyProviderType.add(keyStoreURL);
        }
    }

    public void removeKey(String key) {
        for (KeyValueType keyValueType : new ArrayList<KeyValueType>(this.keyProviderType.getValidatingAlias())) {
            if (keyValueType.getValue() != null && keyValueType.getValue().equals(key)) {
                this.keyProviderType.remove(keyValueType);
            }
        }
    }

    public void addKey(String key, String validatingHost) {
        removeKey(key);

        String[] hosts = validatingHost.split(",");

        for (String host : hosts) {
            KeyValueType kv = new KeyValueType();

            kv.setKey(host.trim());
            kv.setValue(key.trim());

            this.keyProviderType.add(kv);
        }
    }

    public void setKeyProviderType(KeyProviderType keyProviderType, String file, String relativeTo) {
        if (keyProviderType != null) {
            for (KeyValueType kv : this.keyProviderType.getValidatingAlias()) {
                keyProviderType.add(kv);
            }
        }

        this.filePath = file;
        this.relativeTo = relativeTo;
        this.keyProviderType = keyProviderType;
        configureKeyStoreFilePath();
        getFederationService().getValue().setKeyProviderType(this.keyProviderType);
    }
}
