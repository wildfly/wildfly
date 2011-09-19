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

/**
 * @author John Bailey
 */
public class ParsedRelationshipRole {
    String relationshipRoleName;
    Boolean foreignKeyConstraint;
    Boolean batchCascadeDelete;
    ParsedReadAhead readAhead;
    String cmrFieldName;
    String cmrFieldType;
    List<ParsedCmpField> keyFields;

    public String getRelationshipRoleName() {
        return relationshipRoleName;
    }

    public Boolean getForeignKeyConstraint() {
        return foreignKeyConstraint;
    }

    public Boolean getBatchCascadeDelete() {
        return batchCascadeDelete;
    }

    public ParsedReadAhead getReadAhead() {
        return readAhead;
    }

    public String getCmrFieldName() {
        return cmrFieldName;
    }

    public String getCmrFieldType() {
        return cmrFieldType;
    }

    public List<ParsedCmpField> getKeyFields() {
        return keyFields;
    }
}
