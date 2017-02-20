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

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.Protocol;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Singleton factory and transformer for JGroups protocols.
 * @author Paul Ferraro
 */
public enum ProtocolFactory implements UnaryOperator<Protocol> {

    TRANSFORMER;

    private final Map<String, CustomProtocolSource> customProtocols = new HashMap<>();

    ProtocolFactory() {
        short id = 600; // 500-1000 range reserved for "external" protocols.
        for (CustomProtocol protocol : ServiceLoader.load(CustomProtocol.class, CustomProtocol.class.getClassLoader())) {
            this.customProtocols.put(protocol.getName(), new CustomProtocolSource(id++, protocol.getClass()));
        }
    }

    @Override
    public Protocol apply(Protocol protocol) {
        return Optional.ofNullable(this.customProtocols.get(protocol.getName())).map(custom -> custom.get()).orElse(protocol);
    }

    public static <T> T newInstance(Class<? extends T> protocolClass) {
        PrivilegedAction<T> action = () -> {
            try {
                return protocolClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        };
        return WildFlySecurityManager.doUnchecked(action);
    }

    private static class CustomProtocolSource implements Supplier<Protocol> {
        private final Class<? extends CustomProtocol> protocolClass;
        private final short id;

        CustomProtocolSource(short id, Class<? extends CustomProtocol> protocolClass) {
            this.id = id;
            this.protocolClass = protocolClass;
            ClassConfigurator.addProtocol(id, protocolClass);
        }

        @Override
        public Protocol get() {
            return newInstance(this.protocolClass).setId(this.id);
        }
    }
}
