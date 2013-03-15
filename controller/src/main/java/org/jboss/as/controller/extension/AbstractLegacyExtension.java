/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.extension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Abstract superclass for {@link Extension} implementations where the extension is no longer supported
 * for use on current version servers but is supported on host controllers in order to allow use
 * of the extension on legacy version hosts in a mixed-version domain.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public abstract class AbstractLegacyExtension implements Extension {

    private final String extensionName;
    private final List<String> subsystemNames;

    protected AbstractLegacyExtension(String extensionName, String... subsystemNames) {
        this.extensionName = extensionName;
        this.subsystemNames = Arrays.asList(subsystemNames);
    }

    @Override
    public void initialize(ExtensionContext context) {

        if (context.getProcessType() == ProcessType.DOMAIN_SERVER) {
            // Do nothing. This allows an extension=cmp:add op that's really targeted
            // to legacy servers to work
            ControllerLogger.SERVER_MANAGEMENT_LOGGER.ignoringUnsupportedLegacyExtension(subsystemNames, extensionName);
            return;
        } else if (context.getProcessType() == ProcessType.STANDALONE_SERVER) {
            throw new UnsupportedOperationException(ControllerMessages.MESSAGES.unsupportedLegacyExtension(extensionName));
        }

        Set<ManagementResourceRegistration> subsystemRoots = initializeLegacyModel(context);
        for (ManagementResourceRegistration subsystemRoot : subsystemRoots) {
            subsystemRoot.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION,
                new UnsupportedSubsystemDescribeHandler(extensionName));
        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {

        if (context.getProcessType() == ProcessType.DOMAIN_SERVER) {
            // Do nothing. This allows the extension=cmp:add op that's really targeted
            // to legacy servers to work
            return;
        } else if (context.getProcessType() == ProcessType.STANDALONE_SERVER) {
            throw new UnsupportedOperationException(ControllerMessages.MESSAGES.unsupportedLegacyExtension(extensionName));
        }

        initializeLegacyParsers(context);
    }

    /**
     * Perform the work that a non-legacy extension would perform in {@link #initialize(org.jboss.as.controller.ExtensionContext)},
     * except no handler for the {@code describe} operation should be registered.
     *
     * @param context the extension context
     * @return set containing the root {@link ManagementResourceRegistration} for all subsystems that were registered.
     *         The calling method will register a {@code describe} operation handler for each of these
     */
    protected abstract Set<ManagementResourceRegistration> initializeLegacyModel(ExtensionContext context);

    /**
     * Perform the work that a non-legacy extension would perform in
     * {@link #initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)}.
     *
     * @param context the extension parsing context
     */
    protected abstract void initializeLegacyParsers(ExtensionParsingContext context);
}
