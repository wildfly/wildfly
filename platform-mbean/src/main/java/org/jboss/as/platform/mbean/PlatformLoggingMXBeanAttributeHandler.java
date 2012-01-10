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

package org.jboss.as.platform.mbean;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handles read-attribute and write-attribute for the resource representing {@code java.lang.management.PlatformLoggingMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PlatformLoggingMXBeanAttributeHandler extends AbstractPlatformMBeanAttributeHandler {

    public static PlatformLoggingMXBeanAttributeHandler INSTANCE = new PlatformLoggingMXBeanAttributeHandler();

    private PlatformLoggingMXBeanAttributeHandler() {
    }

    @Override
    protected void executeReadAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        /**
         * Implementation note: This implementation uses indirect access to the mbean (i.e. via the
         * {@code MBeanServerConnection} API) in order to avoid adding a compile time dependency on JDK 7. If the base
         * JDK requirement for JBoss AS ever moves to JDK 7, this implementation can be updated to use the
         * {@code java.lang.management.PlatformLoggingMXBean} interface.
         */

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        if (PlatformMBeanConstants.OBJECT_NAME.equals(name)) {
            context.getResult().set(PlatformMBeanConstants.PLATFORM_LOGGING_MXBEAN_NAME);
        } else if (PlatformMBeanConstants.LOGGER_NAMES.equals(name)) {
            String[] names = (String[]) PlatformMBeanUtil.getMBeanAttribute(PlatformMBeanConstants.PLATFORM_LOGGING_OBJECT_NAME, "LoggerNames");
            final ModelNode result = context.getResult();
            result.setEmptyList();
            for (String loggerName : names) {
                result.add(loggerName);
            }
        } else if (PlatformMBeanConstants.LOGGING_READ_ATTRIBUTES.contains(name)) {
            // Bug
            throw PlatformMBeanMessages.MESSAGES.badReadAttributeImpl9(name);
        } else {
            // Shouldn't happen; the global handler should reject
            throw unknownAttribute(operation);
        }

    }

    @Override
    protected void executeWriteAttribute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Shouldn't happen; the global handler should reject
        throw unknownAttribute(operation);

    }

    @Override
    protected void register(ManagementResourceRegistration registration) {

        registration.registerReadOnlyAttribute(PlatformMBeanConstants.OBJECT_NAME, this, AttributeAccess.Storage.RUNTIME);

        for (String attribute : PlatformMBeanConstants.LOGGING_READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attribute, this, AttributeAccess.Storage.RUNTIME);
        }
    }
}
