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
package org.jboss.as.controller.persistence;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * Context passed to {@link XMLElementWriter}s that marshal a model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ModelMarshallingContext {

    /**
     * Gets the model to marshal.
     * @return
     */
    ModelNode getModelNode();

    /**
     * Gets the writer that can marshal the subsystem with the given name.
     *
     * @param subsystemName the name of the subsystem
     * @return the writer, or {@code null} if there is no writer registered
     *          under {@code subsystemName}
     */
    XMLElementWriter<SubsystemMarshallingContext> getSubsystemWriter(String subsystemName);

    /**
     * Gets the writer that can marshal the per-deployment configuration for the
     * subsystem with the given name.
     *
     * @param subsystemName the name of the subsystem
     * @return the writer, or {@code null} if there is no writer registered
     *          under {@code subsystemName}
     */
    XMLElementWriter<SubsystemMarshallingContext> getSubsystemDeploymentWriter(String subsystemName);
}
