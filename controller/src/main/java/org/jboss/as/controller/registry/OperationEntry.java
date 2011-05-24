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

package org.jboss.as.controller.registry;

import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * Information about a registered {@code OperationHandler}.
 *
 * @author Emanuel Muckenhuber
 */
public final class OperationEntry {

    public enum EntryType {
        PUBLIC, PRIVATE;
    }

    private final NewStepHandler operationHandler;
    private final DescriptionProvider descriptionProvider;
    private final EntryType type;
    private final boolean inherited;

    protected OperationEntry(final NewStepHandler operationHandler, final DescriptionProvider descriptionProvider, final boolean inherited, final EntryType type) {
        this.operationHandler = operationHandler;
        this.descriptionProvider = descriptionProvider;
        this.inherited = inherited;
        this.type = type;
    }

    NewStepHandler getOperationHandler() {
        return operationHandler;
    }

    public DescriptionProvider getDescriptionProvider() {
        return descriptionProvider;
    }

    public boolean isInherited() {
        return inherited;
    }

    public EntryType getType() {
        return type;
    }

}
