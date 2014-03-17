/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.core.model.test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.as.server.services.security.VaultReaderException;

/**
 * A mock vault reader implementation.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class TestVaultReader extends AbstractVaultReader {

    private static final Pattern VAULT_PATTERN = Pattern.compile("VAULT::.*::.*::.*");

    private volatile String fqn;
    private final Map<String, Object> options = new HashMap<String, Object>();
    @Override
    protected void createVault(String fqn, Map<String, Object> options) throws VaultReaderException {
        assert this.fqn == null : "vault already initialized";
        this.fqn = fqn == null ? "default" : fqn;
        if (options != null) {
            this.options.putAll(options);
        }
    }

    @Override
    protected void createVault(String fqn, String module,
            Map<String, Object> options) throws VaultReaderException {
        createVault(fqn, options);
    }

    @Override
    protected void destroyVault() {
        this.fqn = null;
        this.options.clear();
    }

    @Override
    public boolean isVaultFormat(String encrypted) {
        return encrypted != null && VAULT_PATTERN.matcher(encrypted).matches();
    }

    @Override
    public String retrieveFromVault(String encrypted) {
        if (isVaultFormat(encrypted)) {

            if (fqn == null) {
                throw new SecurityException("Vault not initialized");
            }

            String[] split = encrypted.split("::");
            if (split[1].equals(fqn)) {
                Object value = options.get(split[2]);
                if (value == null) {
                    value = split[3];
                }
                return value.toString();
            } else {
                throw new SecurityException("Unknown vault " + split[1]);
            }
        }
        return encrypted;
    }
}
