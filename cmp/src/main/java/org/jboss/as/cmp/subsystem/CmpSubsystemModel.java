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

package org.jboss.as.cmp.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * @author John Bailey
 */
class CmpSubsystemModel {
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, CmpExtension.SUBSYSTEM_NAME);
    static final String HILO_KEY_GENERATOR = "hilo-keygenerator";
    static final String UUID_KEY_GENERATOR = "uuid-keygenerator";

    static final String JNDI_NAME = "jndi-name";
    static final String BLOCK_SIZE = "block-size";
    static final String CREATE_TABLE = "create-table";
    static final String CREATE_TABLE_DDL = "create-table-ddl";
    static final String DATA_SOURCE = "data-source";
    static final String DROP_TABLE = "drop-table";
    static final String ID_COLUMN = "id-column";
    static final String SELECT_HI_DDL = "select-hi-ddl";
    static final String SEQUENCE_COLUMN = "sequence-column";
    static final String SEQUENCE_NAME = "sequence-name";
    static final String TABLE_NAME = "table-name";

    static final PathElement HILO_KEY_GENERATOR_PATH =  PathElement.pathElement(HILO_KEY_GENERATOR);
    static final PathElement UUID_KEY_GENERATOR_PATH =  PathElement.pathElement(UUID_KEY_GENERATOR);

}
