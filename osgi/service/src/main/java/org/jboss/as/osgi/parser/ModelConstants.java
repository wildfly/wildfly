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

package org.jboss.as.osgi.parser;


/**
 * An enumeration of the supported OSGi subsystem namespaces.
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 13-Sep-2010
 */
public interface ModelConstants {

    String ACTIVATION = "activation";
    String BUNDLE = "bundle";
    String CAPABILITY = "capability";
    String CONFIGURATION = "configuration";
    String ENTRIES = "entries";
    String FRAMEWORK_PROPERTY = "framework-property";
    String ID = "id";
    String NAME = "name";
    String PID = "pid";
    String STARTLEVEL = "startlevel";
    String STATE = "state";
    String SYMBOLIC_NAME = "symbolic-name";
    String VALUE = "value";
    String VERSION = "version";

}
