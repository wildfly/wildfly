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
package org.jboss.as.test.integration.security.vault;

/**
 * Class that supplies password to the Vault.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>.
 */
public class ExternalPassword {

    private String passwordPart1;
    private String passwordPart2;

    public ExternalPassword() {
        // will return default value
    }

    public ExternalPassword(String part1, String part2) {

        // will return value which is constructed from two parts of password
        this.passwordPart1 = part1;
        this.passwordPart2 = part2;

    }

    public char[] toCharArray() {

        if (passwordPart1 != null && passwordPart2 != null) {

            return (passwordPart1 + passwordPart2).toCharArray();
        }

        // return default value which is identical with BasicVaultServerSetupTask.VAULT_PASSWORD
        // it is returned directly because this class is part of independent module
        return "VaultPassword".toCharArray();

    }
}
