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

package org.jboss.as.cli.impl;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.picketbox.plugins.vault.PicketBoxSecurityVault;

/**
 * @author Alexey Loubyansky
 *
 */
class CLIVaultReader {

    private static final Pattern VAULT_PATTERN = Pattern.compile("VAULT::.*::.*::.*");

    private volatile SecurityVault vault;

    CLIVaultReader() {
    }

    void init(Map<String, Object> options) throws SecurityVaultException {
        vault = new PicketBoxSecurityVault();
        vault.init(options);
    }

    String retrieve(String password) throws SecurityVaultException {
        if(isVaultFormat(password)) {
            char[] retrieved = getValue(password);
            return retrieved != null ? new String(retrieved) : null;
        }
        return password;
    }

    boolean isVaultFormat(String str) {
        return str != null && VAULT_PATTERN.matcher(str).matches();
    }

    private char[] getValue(String vaultString) throws SecurityVaultException {
        if(vault == null) {
            throw new SecurityVaultException("Vault has not been initialized.");
        }
        String[] tokens = tokens(vaultString);
        byte[] sharedKey = null;
        if (tokens.length > 2) {
            // only in case of conversion of old vault implementation
            sharedKey = tokens[3].getBytes(StandardCharsets.UTF_8);
        }
        return vault.retrieve(tokens[1], tokens[2], sharedKey);
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
