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

package org.wildfly.clustering.web.undertow;

import org.jboss.as.clustering.controller.BinaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.BinaryServiceNameFactory;
import org.jboss.as.clustering.controller.BinaryServiceNameFactoryProvider;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Host;

/**
 * @author Paul Ferraro
 */
public enum UndertowBinaryRequirement implements BinaryRequirement, BinaryServiceNameFactoryProvider {
    HOST(Capabilities.CAPABILITY_HOST, Host.class),
    ;
    private final String name;
    private final Class<?> type;
    private final BinaryServiceNameFactory factory = new BinaryRequirementServiceNameFactory(this);

    UndertowBinaryRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public BinaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
