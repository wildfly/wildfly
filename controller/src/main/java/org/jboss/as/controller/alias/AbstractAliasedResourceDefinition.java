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

package org.jboss.as.controller.alias;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

/**
 * Abstract base class for {@link AliasedResourceDefinition} implementations. Provides an implementation of
 * {@link AliasedResourceDefinition#getAliasDescriptionProvider(ImmutableManagementResourceRegistration, PathElement)}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class AbstractAliasedResourceDefinition extends SimpleResourceDefinition implements AliasedResourceDefinition {

    private final StandardResourceDescriptionResolver standardResourceDescriptionResolver;
    protected final OperationStepHandler aliasHandler;

    protected AbstractAliasedResourceDefinition(final PathElement pathElement, final StandardResourceDescriptionResolver descriptionResolver) {
        super(pathElement, descriptionResolver);
        this.standardResourceDescriptionResolver = descriptionResolver;
        aliasHandler = new AliasedResourceOperationStepHandler(PathAddress.pathAddress(pathElement));
    }

    protected AbstractAliasedResourceDefinition(final PathElement pathElement, final StandardResourceDescriptionResolver descriptionResolver,
                                                final OperationStepHandler addHandler, final OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
        this.standardResourceDescriptionResolver = descriptionResolver;
        aliasHandler = new AliasedResourceOperationStepHandler(PathAddress.pathAddress(pathElement));
    }

    protected AbstractAliasedResourceDefinition(final PathElement pathElement, final StandardResourceDescriptionResolver descriptionResolver,
                                                final OperationStepHandler addHandler, final OperationStepHandler removeHandler,
                                                final OperationEntry.Flag addRestartLevel, final OperationEntry.Flag removeRestartLevel) {
        super(pathElement, descriptionResolver, addHandler, removeHandler, addRestartLevel, removeRestartLevel);
        this.standardResourceDescriptionResolver = descriptionResolver;
        aliasHandler = new AliasedResourceOperationStepHandler(PathAddress.pathAddress(pathElement));
    }

    protected OperationStepHandler getAliasHandler() {
        return aliasHandler;
    }

    public void registerAliasOperations(ManagementResourceRegistration resourceRegistration, PathElement alias) {
        registerAddOperation(resourceRegistration, aliasHandler, OperationEntry.Flag.RESTART_NONE);
        registerRemoveOperation(resourceRegistration, aliasHandler, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }


    public void registerAliasAttributes(ManagementResourceRegistration resourceRegistration, PathElement alias){

    }

    @Override
    public void registerAliasChildren(ManagementResourceRegistration resourceRegistration, PathElement alias) {
        // none
    }

    @Override
    public DescriptionProvider getAliasDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration, PathElement alias) {
        DescriptionProvider delegate = new DefaultResourceDescriptionProvider(resourceRegistration, standardResourceDescriptionResolver);
        return new AliasedResourceDescriptionProvider(alias, delegate);
    }

    private class AliasedResourceDescriptionProvider implements DescriptionProvider {
        private final PathElement alias;
        private final DescriptionProvider delegate;


        private AliasedResourceDescriptionProvider(PathElement alias, DescriptionProvider delegate) {
            this.alias = alias;
            this.delegate = delegate;
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode unaliased = delegate.getModelDescription(locale);
            ResourceBundle bundle = standardResourceDescriptionResolver.getResourceBundle(locale);
            if (bundle != null) {
                String bundleKey = standardResourceDescriptionResolver.getKeyPrefix() + "alias." + alias.getKey();
                if (!alias.isWildcard()) {
                    bundleKey += ".";
                    bundleKey += alias.getValue();
                }
                try {
                    unaliased.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString(bundleKey));
                } catch (MissingResourceException ignored) {
                    // the bundle doesn't follow the pattern for storing a key with a special alias description,
                    // so just go with the aliased resource's description
                }
            }
            return unaliased;
        }
    }
}
