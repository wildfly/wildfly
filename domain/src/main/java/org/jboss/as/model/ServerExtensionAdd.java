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

package org.jboss.as.model;

import org.jboss.as.Extension;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerExtensionAdd extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = 3718982114819320314L;

    private final String moduleName;

    public ServerExtensionAdd(final String moduleName) {
        super(true);
        this.moduleName = moduleName;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(final ServerModel element) throws UpdateFailedException {
        element.addExtension(moduleName);
    }

    /** {@inheritDoc} */
    public <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        // no action
    }

    /** {@inheritDoc}
     * @param updateContext*/
    public void applyUpdateBootAction(final UpdateContext updateContext) {
        try {
            for (Extension extension : Module.loadService(moduleName, Extension.class)) {
                extension.activate(null);
            }
        } catch (ModuleLoadException e) {
            // todo
        }
    }

    public ServerExtensionRemove getCompensatingUpdate(final ServerModel original) {
        return new ServerExtensionRemove(moduleName);
    }
}
