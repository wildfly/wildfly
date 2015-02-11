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
package org.jboss.as.clustering.jgroups.subsystem;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;

import org.jboss.as.clustering.jgroups.LogFactory;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jgroups.Global;

/**
 * Registers the JGroups subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JGroupsExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jgroups";

    private static final String RESOURCE_NAME = JGroupsExtension.class.getPackage().getName() + ".LocalDescriptions";

    // Workaround for JGRP-1475
    // Configure JGroups to use jboss-logging.
    static {
        PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                if (System.getProperty(Global.CUSTOM_LOG_FACTORY) == null) {
                    System.setProperty(Global.CUSTOM_LOG_FACTORY, LogFactory.class.getName());
                }
                return null;
            }
        };
        AccessController.doPrivileged(action);
    }

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
           prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JGroupsExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, JGroupsModel.CURRENT.getVersion());

        registration.registerSubsystemModel(new JGroupsSubsystemResourceDefinition(context.isRuntimeOnlyRegistrationValid()));
        registration.registerXMLElementWriter(new JGroupsSubsystemXMLWriter());

        if (context.isRegisterTransformers()) {
            // Register transformers for all but the current model
            for (JGroupsModel model: EnumSet.complementOf(EnumSet.of(JGroupsModel.CURRENT))) {
                ModelVersion version = model.getVersion();
                TransformationDescription.Tools.register(JGroupsSubsystemResourceDefinition.buildTransformers(version), registration, version);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (JGroupsSchema schema: JGroupsSchema.values()) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespaceUri(), new JGroupsSubsystemXMLReader(schema));
        }
    }
}
