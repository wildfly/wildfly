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
package org.jboss.as.controller;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface VaultReader {
    boolean isVaultFormat(String encrypted);

    /**
     * Unmasks vaulted data. Vaulted data has the format {@code VAULT::vault_block::attribute_name::sharedKey}
     *
     * <p>
     * Vault Block acts as the unique id of a block such as "messaging", "security" etc Attribute Name is the name of the
     * attribute whose value we are preserving Shared Key is the key generated by the off line vault during storage of the
     * attribute value
     * </p>
     *
     * @param encrypted the masked data, may be {@code null}
     * @return the unmasked data, or the original data if it is not vault data
     */
    String retrieveFromVault(String encrypted);

}
