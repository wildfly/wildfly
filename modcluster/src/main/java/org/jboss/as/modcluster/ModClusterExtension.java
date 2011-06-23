/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Domain extension used to initialize the mod_cluster subsystem element handlers.
 *
 * @author Jean-Frederic Clere
 */
public class ModClusterExtension implements XMLStreamConstants, Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.modcluster");

    public static final String SUBSYSTEM_NAME = "modcluster";
    public static final String NAMESPACE = "urn:jboss:domain:modcluster:1.0";

    final ModClusterSubsystemElementParser parser = new ModClusterSubsystemElementParser();

    private static final DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {

        log.debugf("Activating Mod_cluster Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(ModClusterSubsystemDescriptionProviders.SUBSYSTEM);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, ModClusterSubsystemAdd.INSTANCE, ModClusterSubsystemAdd.INSTANCE, false);
        registration.registerOperationHandler(DESCRIBE, ModClusterSubsystemDescribe.INSTANCE, ModClusterSubsystemDescribe.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerOperationHandler("list-proxies", ModClusterListProxies.INSTANCE, ModClusterListProxies.INSTANCE, false);

        subsystem.registerXMLElementWriter(parser);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
    }
}
