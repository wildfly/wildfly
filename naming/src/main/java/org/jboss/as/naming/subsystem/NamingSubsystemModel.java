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

package org.jboss.as.naming.subsystem;

import org.jboss.as.controller.PathElement;

/**
 * User: jpai
 */
public interface NamingSubsystemModel {

    String BINDING = "binding";
    String BINDING_TYPE = "binding-type";

    String CLASS ="class";

    String LOOKUP = "lookup";

    String OBJECT_FACTORY = "object-factory";

    String MODULE ="module";

    String SIMPLE = "simple";

    String TYPE = "type";

    String VALUE = "value";

    PathElement BINDING_PATH = PathElement.pathElement(BINDING);


}
