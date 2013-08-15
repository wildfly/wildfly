/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.management;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.registry.AbstractModelResource;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.ResourceProvider;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Alexey Loubyansky
 */
class PatchResource extends AbstractModelResource {


    /**
     * The local model.
     */
    private final ModelNode model = new ModelNode();

    protected PatchResource(ServiceController<InstallationManager> imController) {
        super.registerResourceProvider("layer", new LayerResourceProvider(imController));
        super.registerResourceProvider("addon", new AddOnResourceProvider(imController));
        model.protect();
    }

    @Override
    public ModelNode getModel() {
        return model;
    }

    @Override
    public void writeModel(ModelNode newModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isModelDefined() {
        return model.isDefined();
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    @Override
    public Resource clone() {
        return this;
    }

    class LayerResourceProvider extends ElementProviderResourceProvider {

        LayerResourceProvider(ServiceController<InstallationManager> imController) {
            super(imController);
        }

        @Override
        protected Collection<? extends PatchableTarget> getChildTargets(InstalledIdentity identity) {
            return identity.getLayers();
        }

    }

    class AddOnResourceProvider extends ElementProviderResourceProvider {

        AddOnResourceProvider(ServiceController<InstallationManager> imController) {
            super(imController);
        }

        @Override
        protected Collection<? extends PatchableTarget> getChildTargets(InstalledIdentity identity) {
            return identity.getAddOns();
        }

    }

    abstract class ElementProviderResourceProvider implements ResourceProvider {

        protected final ServiceController<InstallationManager> imController;

        ElementProviderResourceProvider(ServiceController<InstallationManager> imController) {
            if (imController == null) {
                throw new IllegalArgumentException("Installation manager service controller is null"); // internal wrong usage, no i18n
            }
            this.imController = imController;
        }

        protected abstract Collection<? extends PatchableTarget> getChildTargets(InstalledIdentity identity);

        @Override
        public boolean has(String name) {
            return children().contains(name);
        }

        @Override
        public Resource get(String name) {
            return PlaceholderResource.INSTANCE;
        }

        @Override
        public boolean hasChildren() {
            return !children().isEmpty();
        }

        @Override
        public Set<String> children() {
            final InstallationManager manager = imController.getValue();
            final Collection<? extends PatchableTarget> targets = getChildTargets(manager);
            if (targets.isEmpty()) {
                return Collections.emptySet();
            }
            if (targets.size() == 1) {
                final PatchableTarget target = targets.iterator().next();
                return Collections.singleton(target.getName());
            }
            final Set<String> names = new HashSet<String>(targets.size());
            for (PatchableTarget target : targets) {
                names.add(target.getName());
            }
            return names;
        }

        @Override
        public void register(String name, Resource resource) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource remove(String name) {
            throw new UnsupportedOperationException();
        }

    }
}
