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
package org.jboss.as.clustering.infinispan.subsystem;

import java.io.IOException;
import java.io.Serializable;

import org.jboss.as.clustering.jgroups.subsystem.JGroupsExtension;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.staxmapper.XMLMapper;

/**
 * Initializer for the JGroups subsystem.
 * @author Paul Ferraro
 */
public class JGroupsSubsystemInitialization extends AdditionalInitialization implements Serializable {
    private static final long serialVersionUID = -4433079373360352449L;
    private final String module = "jgroups";
    private final RunningMode mode;
    private transient Extension extension = new JGroupsExtension();

    public JGroupsSubsystemInitialization() {
        this(RunningMode.ADMIN_ONLY);
    }

    public JGroupsSubsystemInitialization(RunningMode mode) {
        this.mode = mode;
    }

    @Override
    protected RunningMode getRunningMode() {
        return this.mode;
    }

    @Override
    protected void addParsers(ExtensionRegistry registry, XMLMapper mapper) {
        this.extension.initializeParsers(registry.getExtensionParsingContext(this.module, mapper));
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry registry, Resource rootResource, ManagementResourceRegistration registration) {
        this.extension.initialize(registry.getExtensionContext(this.module, registration, true));
    }

    private void readObject(java.io.ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        this.extension = new JGroupsExtension();
    }
}