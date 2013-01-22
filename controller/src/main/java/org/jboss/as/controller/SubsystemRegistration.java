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

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.CombinedTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.SubsystemTransformer;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * A subsystem registration.
 * <p>
 * If no XML mappings are defined, then a simple empty XML mapping is used.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface SubsystemRegistration {

    /**
     * Get the model node registration for this subsystem.
     *
     * @param descriptionProvider provider of the description of the subsystem's root management resource
     *
     * @return the subsystem-level model node registration
     * @deprecated use {@link SubsystemRegistration#registerSubsystemModel(ResourceDefinition)}
     */
    @Deprecated
    ManagementResourceRegistration registerSubsystemModel(DescriptionProvider descriptionProvider);

    /**
     * Get the model node registration for this subsystem.
     *
     * @param resourceDefinition  factory for the provider of the description of the subsystem's root management resource
     * @return the subsystem-level model node registration
     */
    ManagementResourceRegistration registerSubsystemModel(ResourceDefinition resourceDefinition);

    /**
     * Get the deployment model node registration for this subsystem.
     *
     * @param descriptionProvider  provider of the description of the subsystem's root deployment-level management resource
     * @return the deployment-level model node registration
     * @deprecated use {@link SubsystemRegistration#registerDeploymentModel(ResourceDefinition)}
     */
    @Deprecated
    ManagementResourceRegistration registerDeploymentModel(DescriptionProvider descriptionProvider);

    /**
     * Get the deployment model node registration for this subsystem.
     *
     * @param resourceDefinition factory for the provider of the description of the subsystem's root deployment-level management resource
     * @return the deployment-level model node registration
     */
    ManagementResourceRegistration registerDeploymentModel(ResourceDefinition resourceDefinition);


    /**
     * Registers the {@link XMLElementWriter} that can handle marshalling
     * the subsystem's configuration to XML.
     *
     * @param writer the writer
     */
    void registerXMLElementWriter(XMLElementWriter<SubsystemMarshallingContext> writer);

    /**
     * Register transformers for a specific model versions.
     *
     * @param version the model version range
     * @param resourceTransformer the subsystem resource transformer
     * @return the transformers registry
     */
    TransformersSubRegistration registerModelTransformers(ModelVersionRange version, ResourceTransformer resourceTransformer);

    /**
     * Register transformers for a given model version.
     *
     * @param version the model version
     * @param resourceTransformer the subsystem resource transformer
     * @param operationTransformer the subsystem operation transformer
     * @return the transformers registry
     */
    TransformersSubRegistration registerModelTransformers(ModelVersionRange version, ResourceTransformer resourceTransformer, OperationTransformer operationTransformer);

    /**
     * Register transformers for a given model version.
     *
     * @param version the model version
     * @param combinedTransformer the combined transformer
     * @return the subsystem registration
     */
    TransformersSubRegistration registerModelTransformers(ModelVersionRange version, CombinedTransformer combinedTransformer);

}
