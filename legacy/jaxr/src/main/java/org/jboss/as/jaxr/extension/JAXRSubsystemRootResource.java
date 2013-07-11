/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;


/**
 * The JAXR subsystem root resource
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-Nov-2011
 */
class JAXRSubsystemRootResource extends ModelOnlyResourceDefinition {

    static SimpleAttributeDefinition CONNECTION_FACTORY_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder("jndi-name", ModelType.STRING, true)
            .setAllowExpression(true)
            .build();

    static SimpleAttributeDefinition CONNECTION_FACTORY_IMPL_ATTRIBUTE =
            new SimpleAttributeDefinition("class", ModelType.STRING, true);

    JAXRSubsystemRootResource() {
        super(JAXRExtension.SUBSYSTEM_PATH,
                JAXRExtension.getResolver(),
                CONNECTION_FACTORY_ATTRIBUTE, CONNECTION_FACTORY_IMPL_ATTRIBUTE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration subsystemRoot) {
        super.registerChildren(subsystemRoot);
        // JAXR Properties
        subsystemRoot.registerSubModel(new JAXRPropertyDefinition());
    }

    static void registerTransformerers(SubsystemRegistration subsystem) {

        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CONNECTION_FACTORY_ATTRIBUTE)
            .end();
        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 1, 0));
    }
}
