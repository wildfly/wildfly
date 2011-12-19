/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cli;

/**
 * A representation of the SSL Configuration.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface SSLConfig {

    /**
     * @return The location of the keyStore or null if not specified.
     */
    String getKeyStore();

    /**
     * @return The keyStorePassword or null if not specified.
     */
    String getKeyStorePassword();

    /**
     * @return The location of the trustStore or null if not specified.
     */
    String getTrustStore();

    /**
     * @return The trustStorePassword or null if not specified.
     */
    String getTrustStorePassword();

    /**
     * @return true if the CLI should automatically update the trust store.
     */
    boolean isModifyTrustStore();

}
