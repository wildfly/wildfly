/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller.operations;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.operations.common.AbstractExtensionAddHandler;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Adds an extension into the server runtime.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
// TODO this could likely be folded into the superclass
public class ExtensionAddHandler extends AbstractExtensionAddHandler implements RuntimeOperationHandler {

    private final NewExtensionContext extensionContext;

    public ExtensionAddHandler(final NewExtensionContext extensionContext) {
        if (extensionContext == null) {
            throw new IllegalArgumentException("extensionContext is null");
        }
        this.extensionContext = extensionContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String installExtension(String module, NewOperationContext context) {
        try {
            for (NewExtension extension : Module.loadServiceFromCurrent(ModuleIdentifier.fromString(module), NewExtension.class)) {
                extension.initialize(extensionContext);
            }
            return null;
        } catch (ModuleLoadException e) {
            return e.getLocalizedMessage();
        }
    }

}
