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

import org.jboss.staxmapper.XMLElementWriter;

/**
 * Registry for {@link XMLElementWriter}s that can marshal the configuration
 * for a subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface SubsystemXmlWriterRegistry {

    /**
     * Registers the writer that can marshal to XML the configuration of the
     * named subsystem.
     *
     * @param name the name of the subsystem
     * @param writer the XML writer
     */
    void registerSubsystemWriter(String name, XMLElementWriter<SubsystemMarshallingContext> writer);

    /**
     * Register the writer for the per-deployment configuration for the
     * named subsystem.
     *
     * (TODO: round this out.)
     *
     * @param name the name of the subsystem
     * @param writer the XML writer
     */
    void registerSubsystemDeploymentWriter(String name, XMLElementWriter<SubsystemMarshallingContext> writer);
}
