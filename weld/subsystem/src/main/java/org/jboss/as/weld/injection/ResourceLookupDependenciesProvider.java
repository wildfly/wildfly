/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.weld.injection;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Resource;

import org.jboss.as.ee.structure.EJBAnnotationPropertyReplacement;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.spi.DeploymentUnitDependenciesProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;

/**
 * Adds binder service dependencies for any {@link Resource} lookups within java:jboss namespace that are processed by Weld.
 * @author Paul Ferraro
 */
@MetaInfServices(DeploymentUnitDependenciesProvider.class)
public class ResourceLookupDependenciesProvider implements DeploymentUnitDependenciesProvider {

    private static final DotName RESOURCE_ANNOTATION_NAME = DotName.createSimple(Resource.class.getName());

    @Override
    public Set<ServiceName> getDependencies(DeploymentUnit unit) {
        CompositeIndex index = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        PropertyReplacer replacer = EJBAnnotationPropertyReplacement.propertyReplacer(unit);
        List<AnnotationInstance> annotations = index.getAnnotations(RESOURCE_ANNOTATION_NAME);
        Set<ServiceName> result = !annotations.isEmpty() ? new TreeSet<>() : Collections.emptySet();
        for (AnnotationInstance annotation : annotations) {
            AnnotationValue lookupValue = annotation.value("lookup");
            if (lookupValue != null) {
                String lookup = replacer.replaceProperties(lookupValue.asString());
                try {
                    ServiceName name = ContextNames.bindInfoFor(lookup).getBinderServiceName();
                    if (ContextNames.JBOSS_CONTEXT_SERVICE_NAME.isParentOf(name)) {
                        result.add(name);
                    }
                } catch (RuntimeException e) {
                    // No associated naming store
                }
            }
        }
        return result;
    }
}
