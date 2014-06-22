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

package org.jboss.as.security.service;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.security.SecurityExtension;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;

/**
 * A {@link SecurityVault} service
 *
 * @author Anil Saldhana
 */
public class SecurityVaultService implements Service<SecurityVault> {
    public static final ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("vault");
    protected volatile SecurityVault vault;
    private String vaultClass;
    private Map<String, Object> options = new HashMap<String, Object>();

    public SecurityVaultService(String fqn, Map<String, Object> options) {
        this.vaultClass = fqn;
        this.options.putAll(options);
    }

    @Override
    public SecurityVault getValue() throws IllegalStateException, IllegalArgumentException {
        return vault;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            if (vaultClass == null || vaultClass.isEmpty()) {
                vault = SecurityVaultFactory.get();
            } else {
                vault = SecurityVaultFactory.get(vaultClass);
            }
            vault.init(options);
        } catch (SecurityVaultException e) {
            throw SecurityLogger.ROOT_LOGGER.unableToStartException("SecurityVaultService", e);
        }
    }

    @Override
    public void stop(StopContext context) {
    }
}