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

package org.jboss.as.cmp.jdbc.metadata.parser;

import java.util.List;

import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCTypeMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCUserTypeMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCValueClassMetaData;

/**
 * @author John Bailey
 */
public class ParsedApplication {
    ParsedEntity defaultEntity;
    List<ParsedRelationship> relationships = null;
    List<ParsedEntity> entities = null;
    List<JDBCUserTypeMappingMetaData> userTypeMappings = null;
    List<JDBCEntityCommandMetaData> entityCommands;
    List<JDBCValueClassMetaData> valueClasses;
    List<JDBCTypeMappingMetaData> typeMappings;

    public ParsedEntity getDefaultEntity() {
        return defaultEntity;
    }

    public List<ParsedRelationship> getRelationships() {
        return relationships;
    }

    public List<ParsedEntity> getEntities() {
        return entities;
    }

    public List<JDBCUserTypeMappingMetaData> getUserTypeMappings() {
        return userTypeMappings;
    }

    public List<JDBCEntityCommandMetaData> getEntityCommands() {
        return entityCommands;
    }

    public List<JDBCValueClassMetaData> getValueClasses() {
        return valueClasses;
    }

    public List<JDBCTypeMappingMetaData> getTypeMappings() {
        return typeMappings;
    }
}
