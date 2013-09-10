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

package org.jboss.as.domain.management.security;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service to handle loading Keystores from file so that the Keystore can be injected ready for SSLContext creation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class FileKeystoreService implements Service<FileKeystore> {

    private volatile FileKeystore theKeyStore;
    private final String path;
    private final char[] keystorePassword;
    /*
     * The next to values are only applicable when loading a keystore as a keystore.
     */
    private final String alias;
    private final char[] keyPassword;

    private final InjectedValue<String> relativeTo = new InjectedValue<String>();

    public FileKeystoreService(final String path, final char[] keystorePassword, final String alias, final char[] keyPassword) {
        this.path = path;
        this.keystorePassword = keystorePassword;
        this.alias = alias;
        this.keyPassword = keyPassword;
    }

    public void start(StartContext ctx) throws StartException {
        String relativeTo = this.relativeTo.getOptionalValue();
        String file = relativeTo == null ? path : relativeTo + "/" + path;
        final FileKeystore fileKeystore = new FileKeystore(file, keystorePassword, keyPassword, alias);
        fileKeystore.load();
        theKeyStore = fileKeystore;
    }

    public void stop(StopContext ctx) {
        theKeyStore = null;
    }

    public FileKeystore getValue() throws IllegalStateException, IllegalArgumentException {
        return theKeyStore;
    }

    public InjectedValue<String> getRelativeToInjector() {
        return relativeTo;
    }

}
