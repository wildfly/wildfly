/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.ejbql;

import java.util.List;
import org.jboss.as.cmp.bridge.CMPFieldBridge;
import org.jboss.as.cmp.bridge.CMRFieldBridge;
import org.jboss.as.cmp.bridge.EntityBridge;
import org.jboss.as.cmp.bridge.FieldBridge;

/**
 * This abstract syntax node represents a path declaration.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class ASTPath extends SimpleNode {
    public List pathList;
    public List fieldList;
    public int type;

    public boolean innerJoin;

    public ASTPath(int id) {
        super(id);
    }

    public String getPath() {
        return (String) pathList.get(pathList.size() - 1);
    }

    public String getPath(int i) {
        return (String) pathList.get(i);
    }

    public FieldBridge getField() {
        return (FieldBridge) fieldList.get(fieldList.size() - 1);
    }

    public boolean isCMPField() {
        return fieldList.get(fieldList.size() - 1) instanceof CMPFieldBridge;
    }

    public CMPFieldBridge getCMPField() {
        return (CMPFieldBridge) fieldList.get(fieldList.size() - 1);
    }

    public boolean isCMRField() {
        return fieldList.get(fieldList.size() - 1) instanceof CMRFieldBridge;
    }

    public boolean isCMRField(int i) {
        return fieldList.get(i) instanceof CMRFieldBridge;
    }

    public CMRFieldBridge getCMRField() {
        return (CMRFieldBridge) fieldList.get(fieldList.size() - 1);
    }

    public CMRFieldBridge getCMRField(int i) {
        return (CMRFieldBridge) fieldList.get(i);
    }

    public EntityBridge getEntity() {
        Object field = fieldList.get(fieldList.size() - 1);
        if (field instanceof CMRFieldBridge) {
            return ((CMRFieldBridge) field).getRelatedEntity();
        } else if (field instanceof EntityBridge) {
            return (EntityBridge) field;
        } else {
            return null;
        }
    }

    public EntityBridge getEntity(int i) {
        Object field = fieldList.get(i);
        if (field instanceof CMRFieldBridge) {
            return ((CMRFieldBridge) field).getRelatedEntity();
        } else if (field instanceof EntityBridge) {
            return (EntityBridge) field;
        } else {
            return null;
        }
    }

    public int size() {
        return fieldList.size();
    }

    public String toString() {
        return pathList.get(pathList.size() - 1) + " <" + type + ">";
    }

    public boolean equals(Object o) {
        if (o instanceof ASTPath) {
            ASTPath path = (ASTPath) o;
            return path.getPath().equals(getPath());
        }
        return false;
    }

    public int hashCode() {
        return getPath().hashCode();
    }

    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(JBossQLParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
