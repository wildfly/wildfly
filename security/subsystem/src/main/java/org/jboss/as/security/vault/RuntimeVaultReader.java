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
package org.jboss.as.security.vault;

import static java.security.AccessController.doPrivileged;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.as.server.services.security.VaultReaderException;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.action.GetModuleClassLoaderAction;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RuntimeVaultReader extends AbstractVaultReader {

    private static final Pattern VAULT_PATTERN = Pattern.compile("VAULT::.*::.*::.*");

    private volatile SecurityVault vault;


    /**
     * This constructor should remain protected to keep the vault as invisible
     * as possible, but it needs to be exposed for service plug-ability.
     */
    public RuntimeVaultReader() {
    }

    protected void createVault(final String fqn, final Map<String, Object> options) throws VaultReaderException {
        createVault(fqn, null, options);
    }

    protected void createVault(final String fqn, final String module, final Map<String, Object> options) throws VaultReaderException {
        Map<String, Object> vaultOptions = new HashMap<String, Object>(options);
        SecurityVault vault = null;
        try {
            vault = AccessController.doPrivileged(new PrivilegedExceptionAction<SecurityVault>() {
                @Override
                public SecurityVault run() throws Exception {
                    if (fqn == null || fqn.isEmpty()) {
                        return SecurityVaultFactory.get();
                    } else if (module == null ){
                        return SecurityVaultFactory.get(fqn);
                    } else {
                        return SecurityVaultFactory.get(getModuleClassLoader(module), fqn);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable t = e.getCause();
            if (t instanceof SecurityVaultException) {
                throw SecurityLogger.ROOT_LOGGER.vaultReaderException(t);
            }
            if (t instanceof RuntimeException) {
                throw SecurityLogger.ROOT_LOGGER.runtimeException(t);
            }
            throw SecurityLogger.ROOT_LOGGER.runtimeException(t);
        }
        try {
            vault.init(vaultOptions);
        } catch (SecurityVaultException e) {
            throw SecurityLogger.ROOT_LOGGER.vaultReaderException(e);
        }
        this.vault = vault;
    }

    protected void destroyVault() {
        //TODO - there are no cleanup methods in the vault itself
        vault = null;
    }

    public String retrieveFromVault(final String password) throws SecurityException {
        if (isVaultFormat(password)) {

            if (vault == null) {
                throw SecurityLogger.ROOT_LOGGER.vaultNotInitializedException();
            }

            try {
                return getValueAsString(password);
            } catch (SecurityVaultException e) {
                throw SecurityLogger.ROOT_LOGGER.securityException(e);
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
        byte[] sharedKey = null;
        if (tokens.length > 2) {
            // only in case of conversion of old vault implementation
            sharedKey = tokens[3].getBytes(VaultSession.CHARSET);
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

    private ModuleClassLoader getModuleClassLoader(final String moduleSpec) throws ModuleLoadException {
        ModuleLoader loader = Module.getCallerModuleLoader();
        final Module module = loader.loadModule(ModuleIdentifier.fromString(moduleSpec));
        return WildFlySecurityManager.isChecking() ? doPrivileged(new GetModuleClassLoaderAction(module)) : module.getClassLoader();
    }
}
