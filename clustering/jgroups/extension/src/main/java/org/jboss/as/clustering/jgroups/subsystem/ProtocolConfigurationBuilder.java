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

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.modules.ModuleIdentifier;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;

/**
 * @author Paul Ferraro
 */
public class ProtocolConfigurationBuilder extends AbstractProtocolConfigurationBuilder<ProtocolConfiguration> {

    public ProtocolConfigurationBuilder(String stackName, String name) {
        super(stackName, name);
    }

    @Override
    public ProtocolConfigurationBuilder setModule(ModuleIdentifier module) {
        super.setModule(module);
        return this;
    }

    @Override
    public ProtocolConfigurationBuilder setSocketBinding(String socketBindingName) {
        super.setSocketBinding(socketBindingName);
        return this;
    }

    @Override
    public ProtocolConfigurationBuilder addProperty(String name, String value) {
        super.addProperty(name, value);
        return this;
    }

    @Override
    public ProtocolConfiguration getValue() {
        return this;
    }
}
