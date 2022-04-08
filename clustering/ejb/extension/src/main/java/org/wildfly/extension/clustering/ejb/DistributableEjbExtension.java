/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.clustering.ejb;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.Schema;
import org.jboss.as.clustering.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.kohsuke.MetaInfServices;

/**
 * Defines the extension for the distributable-ejb subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@MetaInfServices(Extension.class)
public class DistributableEjbExtension implements Extension {

    static final String SUBSYSTEM_NAME = "distributable-ejb";
    static final SubsystemResourceDescriptionResolver SUBSYSTEM_RESOLVER = new DistributableEjbResourceDescriptionResolver();

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, DistributableEjbModel.CURRENT.getVersion());
        // register the current subsystem resource distributable-ejb
        new DistributableEjbResourceDefinition().register(registration);
        registration.registerXMLElementWriter(new DistributableEjbXMLParser(DistributableEjbSchema.CURRENT));
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Schema<DistributableEjbSchema> schema : EnumSet.allOf(DistributableEjbSchema.class)) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespaceUri(), new DistributableEjbXMLParser(schema));
        }
    }
}
