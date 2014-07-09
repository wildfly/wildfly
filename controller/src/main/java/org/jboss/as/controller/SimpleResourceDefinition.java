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

package org.jboss.as.controller;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ConstrainedResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceAddDescriptionProvider;
import org.jboss.as.controller.descriptions.DefaultResourceDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * Basic implementation of {@link ResourceDefinition}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SimpleResourceDefinition implements ConstrainedResourceDefinition {

    private static final EnumSet<OperationEntry.Flag> RESTART_FLAGS = EnumSet.of(OperationEntry.Flag.RESTART_NONE,
            OperationEntry.Flag.RESTART_RESOURCE_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_JVM);

    private final PathElement pathElement;
    private final ResourceDescriptionResolver descriptionResolver;
    private final DescriptionProvider descriptionProvider;
    private final OperationStepHandler addHandler;
    private final OperationStepHandler removeHandler;
    private final OperationEntry.Flag addRestartLevel;
    private final OperationEntry.Flag removeRestartLevel;
    private final DeprecationData deprecationData;

    /**
     * {@link ResourceDefinition} that uses the given {code descriptionProvider} to describe the resource.
     *
     * @param pathElement         the path. Can be {@code null}.
     * @param descriptionProvider the description provider. Cannot be {@code null}
     * @throws IllegalArgumentException if {@code descriptionProvider} is {@code null}.
     * @deprecated
     */
    @Deprecated
    public SimpleResourceDefinition(final PathElement pathElement, final DescriptionProvider descriptionProvider) {
        if (descriptionProvider == null) {
            throw MESSAGES.nullVar("descriptionProvider");
        }
        this.pathElement = pathElement;
        this.descriptionResolver = null;
        this.descriptionProvider = descriptionProvider;
        this.addHandler = null;
        this.removeHandler = null;
        this.addRestartLevel = null;
        this.removeRestartLevel = null;
        this.deprecationData = null;
    }

    /**
     * {@link ResourceDefinition} that uses the given {code descriptionResolver} to configure a
     * {@link DefaultResourceDescriptionProvider} to describe the resource.
     *
     * @param pathElement         the path. Cannot be {@code null}.
     * @param descriptionResolver the description resolver to use in the description provider. Cannot be {@code null}
     * @throws IllegalArgumentException if any parameter is {@code null}.
     */
    public SimpleResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver) {
        this(pathElement, descriptionResolver, null, null, OperationEntry.Flag.RESTART_NONE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES, null);
    }

    /**
     * {@link ResourceDefinition} that uses the given {code descriptionResolver} to configure a
     * {@link DefaultResourceDescriptionProvider} to describe the resource.
     *
     * @param pathElement         the path. Cannot be {@code null}.
     * @param descriptionResolver the description resolver to use in the description provider. Cannot be {@code null}      *
     * @param addHandler          a handler to {@link #registerOperations(ManagementResourceRegistration) register} for the resource "add" operation.
     *                            Can be {null}
     * @param removeHandler       a handler to {@link #registerOperations(ManagementResourceRegistration) register} for the resource "remove" operation.
     *                            Can be {null}
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public SimpleResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver,
                                    final OperationStepHandler addHandler, final OperationStepHandler removeHandler) {
        this(pathElement, descriptionResolver, addHandler, removeHandler, OperationEntry.Flag.RESTART_NONE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES, null);
    }

    /**
     * {@link ResourceDefinition} that uses the given {code descriptionResolver} to configure a
     * {@link DefaultResourceDescriptionProvider} to describe the resource.
     *
     * @param pathElement         the path. Cannot be {@code null}.
     * @param descriptionResolver the description resolver to use in the description provider. Cannot be {@code null}      *
     * @param addHandler          a handler to {@link #registerOperations(ManagementResourceRegistration) register} for the resource "add" operation.
     *                            Can be {null}
     * @param removeHandler       a handler to {@link #registerOperations(ManagementResourceRegistration) register} for the resource "remove" operation.
     *                            Can be {null}
     * @param deprecationData     Information describing deprecation of this resource. Can be {@code null} if the resource isn't deprecated.
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public SimpleResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver,
                                    final OperationStepHandler addHandler, final OperationStepHandler removeHandler,
                                    final DeprecationData deprecationData) {
        this(pathElement, descriptionResolver, addHandler, removeHandler, OperationEntry.Flag.RESTART_NONE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES, deprecationData);
    }


    /**
     * {@link ResourceDefinition} that uses the given {code descriptionResolver} to configure a
     * {@link DefaultResourceDescriptionProvider} to describe the resource.
     *
     * @param pathElement         the path. Can be {@code null}.
     * @param descriptionResolver the description resolver to use in the description provider. Cannot be {@code null}      *
     * @param addHandler          a handler to {@link #registerOperations(ManagementResourceRegistration) register} for the resource "add" operation.
     *                            Can be {null}
     * @param removeHandler       a handler to {@link #registerOperations(ManagementResourceRegistration) register} for the resource "remove" operation.
     *                            Can be {null}
     * @throws IllegalArgumentException if {@code descriptionResolver} is {@code null}.
     */
    public SimpleResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver,
                                    final OperationStepHandler addHandler, final OperationStepHandler removeHandler,
                                    final OperationEntry.Flag addRestartLevel, final OperationEntry.Flag removeRestartLevel) {
        this(pathElement, descriptionResolver, addHandler, removeHandler, addRestartLevel, removeRestartLevel, null);
    }


    /**
     * {@link ResourceDefinition} that uses the given {code descriptionResolver} to configure a
     * {@link DefaultResourceDescriptionProvider} to describe the resource.
     *
     * @param pathElement         the path. Can be {@code null}.
     * @param descriptionResolver the description resolver to use in the description provider. Cannot be {@code null}      *
     * @param addHandler          a handler to {@link #registerOperations(ManagementResourceRegistration) register} for the resource "add" operation.
     *                            Can be {null}
     * @param removeHandler       a handler to {@link #registerOperations(ManagementResourceRegistration) register} for the resource "remove" operation.
     *                            Can be {null}
     * @param deprecationData     Information describing deprecation of this resource. Can be {@code null} if the resource isn't deprecated.
     * @throws IllegalArgumentException if {@code descriptionResolver} is {@code null}.
     */
    public SimpleResourceDefinition(final PathElement pathElement, final ResourceDescriptionResolver descriptionResolver,
                                    final OperationStepHandler addHandler, final OperationStepHandler removeHandler,
                                    final OperationEntry.Flag addRestartLevel, final OperationEntry.Flag removeRestartLevel,
                                    final DeprecationData deprecationData) {
        if (descriptionResolver == null) {
            throw MESSAGES.nullVar("descriptionProvider");
        }
        this.pathElement = pathElement;
        this.descriptionResolver = descriptionResolver;
        this.descriptionProvider = null;
        this.addHandler = addHandler;
        this.removeHandler = removeHandler;
        this.addRestartLevel = addRestartLevel == null ? OperationEntry.Flag.RESTART_NONE : validateRestartLevel("addRestartLevel", addRestartLevel);
        this.removeRestartLevel = removeRestartLevel == null
                ? OperationEntry.Flag.RESTART_ALL_SERVICES
                : validateRestartLevel("removeRestartLevel", removeRestartLevel);
        this.deprecationData = deprecationData;
    }

    @Override
    public PathElement getPathElement() {
        return pathElement;
    }

    @Override
    public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration) {
        return descriptionProvider == null
                ? new DefaultResourceDescriptionProvider(resourceRegistration, descriptionResolver, deprecationData)
                : descriptionProvider;
    }

    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        if (addHandler != null) {
            registerAddOperation(resourceRegistration, addHandler, addRestartLevel);
        }
        if (removeHandler != null) {
            registerRemoveOperation(resourceRegistration, removeHandler, removeRestartLevel);
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        // no-op
    }

    @Override
    public void registerNotifications(ManagementResourceRegistration resourceRegistration) {
        // no-op
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        // no-op
    }

    /**
     * Gets the {@link ResourceDescriptionResolver} used by this resource definition, or {@code null}
     * if a {@code ResourceDescriptionResolver} is not used.
     *
     * @return the resource description resolver, or {@code null}
     */
    public ResourceDescriptionResolver getResourceDescriptionResolver() {
        return descriptionResolver;
    }

    /**
     * Registers add operation
     *
     * @param registration resource on which to register
     * @param handler      operation handler to register
     * @param flags        with flags
     * @deprecated use {@link #registerAddOperation(org.jboss.as.controller.registry.ManagementResourceRegistration, AbstractAddStepHandler, org.jboss.as.controller.registry.OperationEntry.Flag...)}
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    protected void registerAddOperation(final ManagementResourceRegistration registration, final OperationStepHandler handler, OperationEntry.Flag... flags) {
        if (handler instanceof DescriptionProvider) {
            registration.registerOperationHandler(ModelDescriptionConstants.ADD, handler, (DescriptionProvider) handler, getFlagsSet(flags));
        } else {
            registration.registerOperationHandler(ModelDescriptionConstants.ADD, handler, new DefaultResourceAddDescriptionProvider(registration, descriptionResolver), getFlagsSet(flags));
        }
    }

    /**
     * Registers add operation
     * <p/>
     * Registers add operation
     *
     * @param registration resource on which to register
     * @param handler      operation handler to register
     * @param flags        with flags
     */
    @SuppressWarnings("deprecation")
    protected void registerAddOperation(final ManagementResourceRegistration registration, final AbstractAddStepHandler handler,
                                        OperationEntry.Flag... flags) {
        this.registerAddOperation(registration, (OperationStepHandler) handler, flags);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected void registerRemoveOperation(final ManagementResourceRegistration registration, final OperationStepHandler handler,
                                           OperationEntry.Flag... flags) {

        if (handler instanceof DescriptionProvider) {
            registration.registerOperationHandler(ModelDescriptionConstants.REMOVE, handler, (DescriptionProvider) handler, getFlagsSet(flags));
        } else {
            OperationDefinition opDef = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REMOVE, descriptionResolver)
                    .withFlags(flags)
                    .build();
            registration.registerOperationHandler(opDef, handler);
        }
    }

    @SuppressWarnings("deprecation")
    protected void registerRemoveOperation(final ManagementResourceRegistration registration, final AbstractRemoveStepHandler handler,
                                           OperationEntry.Flag... flags) {
        registerRemoveOperation(registration, (OperationStepHandler) handler, flags);
    }

    private static OperationEntry.Flag validateRestartLevel(String paramName, OperationEntry.Flag flag) {
        if (flag != null && !RESTART_FLAGS.contains(flag)) {
            throw MESSAGES.invalidParameterValue(flag, paramName, RESTART_FLAGS);
        }
        return flag;
    }

    protected static EnumSet<OperationEntry.Flag> getFlagsSet(OperationEntry.Flag... vararg) {
        return SimpleOperationDefinitionBuilder.getFlagsSet(vararg);
    }

    /**
     * {@inheritDoc}
     *
     * @return this default implementation simply returns an empty list.
     */
    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Collections.emptyList();
    }
}
