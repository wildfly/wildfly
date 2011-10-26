/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server.services.security;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.jboss.as.controller.VaultReader;
import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RuntimeVaultReader implements VaultReader {

    private static final Pattern VAULT_PATTERN = Pattern.compile("VAULT::.*::.*::.*");

    private volatile SecurityVault vault;


    /**
     * This constructor should remain protected to keep the vault as invisible
     * as possible
     */
    protected RuntimeVaultReader() {
    }

    void createVault(final String fqn, final Map<String, Object> options) throws SecurityVaultException {
        Map<String, Object> vaultOptions = new HashMap<String, Object>(options);
        SecurityVault vault = null;
        try {
            vault = AccessController.doPrivileged(new PrivilegedExceptionAction<SecurityVault>() {
                @Override
                public SecurityVault run() throws Exception {
                    if (fqn == null || fqn.isEmpty()) {
                        return SecurityVaultFactory.get();
                    } else {
                        return SecurityVaultFactory.get(fqn);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable t = e.getCause();
            if (t instanceof SecurityVaultException) {
                throw (SecurityVaultException)t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            throw new RuntimeException(t);
        }
        vault.init(vaultOptions);
        this.vault = vault;
    }

    void destroyVault() {
        //TODO - there are no cleanup methods in the vault itself
        vault = null;
    }

    public String retrieveFromVault(final String password) throws SecurityException {
        if (isVaultFormat(password)) {

            if (vault == null) {
                throw new SecurityException("Vault is not initialized");
            }

            try {
                return getValueAsString(password);
            } catch (SecurityVaultException e) {
                throw new SecurityException(e);
            }

        }
        return password;
    }

    private String getValueAsString(String vaultString) throws SecurityVaultException {
        char[] val = getValue(vaultString);
        if (val != null)
            return new String(val);
        return null;
    }

    public boolean isVaultFormat(String str) {
        return str != null && VAULT_PATTERN.matcher(str).matches();
    }

    private char[] getValue(String vaultString) throws SecurityVaultException {
        String[] tokens = tokens(vaultString);
        return vault.retrieve(tokens[1], tokens[2], tokens[3].getBytes());
    }

    private String[] tokens(String vaultString) {
        StringTokenizer tokenizer = new StringTokenizer(vaultString, "::");
        int length = tokenizer.countTokens();
        String[] tokens = new String[length];

        int index = 0;
        while (tokenizer != null && tokenizer.hasMoreTokens()) {
            tokens[index++] = tokenizer.nextToken();
        }
        return tokens;
    }
}
