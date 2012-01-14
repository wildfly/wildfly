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

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Registers the JGroups subsystem.
 * @author Paul Ferraro
 */
public class JGroupsExtension implements Extension, DescriptionProvider {

    static final String SUBSYSTEM_NAME = "jgroups";

    private static final PathElement stacksPath = PathElement.pathElement(ModelKeys.STACK);
    private static final JGroupsSubsystemAdd add = new JGroupsSubsystemAdd();
    private static final JGroupsSubsystemRemove remove = new JGroupsSubsystemRemove();
    private static final JGroupsSubsystemDescribe describe = new JGroupsSubsystemDescribe();
    private static final ProtocolStackAdd stackAdd = new ProtocolStackAdd();
    private static final ProtocolStackRemove stackRemove = new ProtocolStackRemove();
    private static final DescriptionProvider stackDescription = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolStackDescription(locale);
        }
    };

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, Namespace.CURRENT.getMajorVersion(), Namespace.CURRENT.getMinorVersion());
        subsystem.registerXMLElementWriter(new JGroupsSubsystemXMLWriter());

        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(this);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, add, add, false);
        registration.registerOperationHandler(ModelDescriptionConstants.REMOVE, remove, remove, false);
        registration.registerOperationHandler(ModelDescriptionConstants.DESCRIBE, describe, describe, false, EntryType.PRIVATE);

        ManagementResourceRegistration stacks = registration.registerSubModel(stacksPath, stackDescription);
        stacks.registerOperationHandler(ModelDescriptionConstants.ADD, stackAdd, stackAdd, false);
        stacks.registerOperationHandler(ModelDescriptionConstants.REMOVE, stackRemove, stackRemove, false);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace: Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), reader);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return JGroupsDescriptions.getSubsystemDescription(locale);
    }
}
