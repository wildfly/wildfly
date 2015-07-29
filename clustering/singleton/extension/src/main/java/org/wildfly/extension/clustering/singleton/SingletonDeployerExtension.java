/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;

/**
 * Extension point for singleton deployer subsystem.
 * @author Paul Ferraro
 */
public class SingletonDeployerExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "singleton-deployer";

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, SingletonDeployerModel.CURRENT.getVersion());
        registration.registerSubsystemModel(new SingletonDeployerResourceDefinition());
        registration.registerXMLElementWriter(new SingletonDeployerXMLWriter());
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (SingletonDeployerSchema schema: SingletonDeployerSchema.values()) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespaceUri(), new SingletonDeployerXMLReader(schema));
        }
    }
}
