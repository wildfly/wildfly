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

import java.security.KeyPair;
import java.security.KeyStore;

import javax.crypto.Cipher;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.kohsuke.MetaInfServices;

/**
 * {@link org.jgroups.protocols.ASYM_ENCRYPT} override that uses a private/public key pair from an injected key store.
 * @author Paul Ferraro
 */
@MetaInfServices(CustomProtocol.class)
public class ASYM_ENCRYPT extends org.jgroups.protocols.ASYM_ENCRYPT implements EncryptProtocol {

    private volatile KeyStore store;
    private volatile String alias;
    private volatile KeyStore.ProtectionParameter password;

    @Override
    public void setKeyStore(KeyStore store) {
        this.store = store;
    }

    @Override
    public void setKeyAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public void setKeyPassword(KeyStore.ProtectionParameter password) {
        this.password = password;
    }

    @Override
    protected void initKeyPair() throws Exception {
        KeyStore store = this.store;
        String alias = this.alias;
        if (!store.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
            throw JGroupsLogger.ROOT_LOGGER.privateKeyStoreEntryExpected(alias);
        }
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) store.getEntry(alias, this.password);
        this.key_pair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
        String provider = this.provider;
        this.asym_cipher = (provider != null) ? Cipher.getInstance(this.asym_algorithm, provider) : Cipher.getInstance(this.asym_algorithm);
        this.asym_cipher.init(Cipher.DECRYPT_MODE, this.key_pair.getPublic());
    }
}
