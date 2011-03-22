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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ResourceAdapterAnnotationProcessor extends AbstractAnnotationEJBProcessor<MessageDrivenComponentDescription> {
    private static final DotName RESOURCE_ADAPTER_ANNOTATION_NAME = DotName.createSimple(ResourceAdapter.class.getName());
    @Override
    protected Class<MessageDrivenComponentDescription> getComponentDescriptionType() {
        return MessageDrivenComponentDescription.class;
    }

    @Override
    protected void processAnnotations(ClassInfo classInfo, CompositeIndex index, MessageDrivenComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations == null)
            return;
        List<AnnotationInstance> annotations = classAnnotations.get(RESOURCE_ADAPTER_ANNOTATION_NAME);
        if (annotations != null) {
            assert annotations.size() == 1 : "@ResourceAdapter can only be on the class itself";
            componentDescription.setResourceAdapterName(annotations.get(0).value().asString());
        }
    }
}
