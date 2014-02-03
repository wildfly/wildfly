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

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.PathElement;

/**
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public interface EESubsystemModel {

    String EAR_SUBDEPLOYMENTS_ISOLATED = "ear-subdeployments-isolated";
    String SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT = "spec-descriptor-property-replacement";
    String JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT = "jboss-descriptor-property-replacement";
    String ANNOTATION_PROPERTY_REPLACEMENT = "annotation-property-replacement";

    String DEFAULT_BINDINGS = "default-bindings";

    String CONTEXT_SERVICE = "context-service";
    String MANAGED_THREAD_FACTORY = "managed-thread-factory";
    String MANAGED_EXECUTOR_SERVICE = "managed-executor-service";
    String MANAGED_SCHEDULED_EXECUTOR_SERVICE = "managed-scheduled-executor-service";
    String SERVICE = "service";

    PathElement DEFAULT_BINDINGS_PATH = PathElement.pathElement(SERVICE,DEFAULT_BINDINGS);

}
