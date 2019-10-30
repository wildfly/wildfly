/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow;

import java.security.PrivilegedAction;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.web.IdentifierSerializer;
import org.wildfly.clustering.web.IdentifierSerializerProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentifierSerializerProvider.class)
public class UndertowIdentifierSerializerProvider implements IdentifierSerializerProvider, PrivilegedAction<String> {

    @Override
    public IdentifierSerializer getSerializer() {
        // Disable session ID marshalling optimization for custom alphabets
        String customAlphabet = WildFlySecurityManager.doUnchecked(this);
        return (customAlphabet == null) ? IdentifierSerializer.BASE64 : IdentifierSerializer.UTF8;
    }

    @Override
    public String run() {
        return System.getProperty("io.undertow.server.session.SecureRandomSessionIdGenerator.ALPHABET");
    }
}
