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

/**
 * Configuration interface for encryption protocols.
 * @author Paul Ferraro
 */
public interface EncryptProtocol extends CustomProtocol {
    /**
     * Sets the key store from which to obtain the encryption key.
     * @param store a key store
     */
    void setKeyStore(KeyStore store);

    /**
     * Sets the alias of the key store entry containing the encryption key.
     * @param alias the key alias
     */
    void setKeyAlias(String alias);

    /**
     * Sets the protection parameter with the key entry is protected.
     * @param protection a protection parameter
     */
    void setKeyPassword(KeyStore.ProtectionParameter protection);
}
