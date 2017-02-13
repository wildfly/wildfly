/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.protocol;

import java.security.KeyStore;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.kohsuke.MetaInfServices;

/**
 * {@link org.jgroups.protocols.SYM_ENCRYPT} override that obtains secret key from an injected key store.
 * @author Paul Ferraro
 */
@MetaInfServices(CustomProtocol.class)
public class SYM_ENCRYPT extends org.jgroups.protocols.SYM_ENCRYPT implements EncryptProtocol {

    private volatile KeyStore store;
    private volatile KeyStore.ProtectionParameter password;

    @Override
    public void setKeyStore(KeyStore store) {
        this.store = store;
    }

    @Override
    public void setKeyAlias(String alias) {
        this.alias(alias);
    }

    @Override
    public void setKeyPassword(KeyStore.ProtectionParameter password) {
        this.password = password;
    }

    @Override
    protected void readSecretKeyFromKeystore() throws Exception {
        KeyStore store = this.store;
        String alias = this.alias();
        if (!store.entryInstanceOf(alias, KeyStore.SecretKeyEntry.class)) {
            throw JGroupsLogger.ROOT_LOGGER.secretKeyStoreEntryExpected(alias);
        }
        KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) store.getEntry(alias, this.password);
        this.secret_key = entry.getSecretKey();
        this.sym_algorithm = this.secret_key.getAlgorithm();
    }
}
