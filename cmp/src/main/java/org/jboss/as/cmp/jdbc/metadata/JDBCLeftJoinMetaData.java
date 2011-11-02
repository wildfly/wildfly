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
package org.jboss.as.cmp.jdbc.metadata;

import java.util.List;

/**
 * Represents
 * <left-join cmr-field="lineItems">
 * <left-join cmr-field="product" eager-load-group="product"/>
 * </left-join>
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public final class JDBCLeftJoinMetaData {
    private String cmrField;
    private String eagerLoadGroup;
    private List<JDBCLeftJoinMetaData> leftJoinList;

    public String getCmrField() {
        return cmrField;
    }

    public String getEagerLoadGroup() {
        return eagerLoadGroup;
    }

    public List<JDBCLeftJoinMetaData> getLeftJoins() {
        return leftJoinList;
    }

    public boolean equals(Object o) {
        boolean result;
        if (o == this) {
            result = true;
        } else if (o instanceof JDBCLeftJoinMetaData) {
            JDBCLeftJoinMetaData other = (JDBCLeftJoinMetaData) o;
            result =
                    (cmrField == null ? other.cmrField == null : cmrField.equals(other.cmrField)) &&
                            (eagerLoadGroup == null ? other.eagerLoadGroup == null : eagerLoadGroup.equals(other.eagerLoadGroup)) &&
                            (leftJoinList == null ? other.leftJoinList == null : leftJoinList.equals(other.leftJoinList));
        } else {
            result = false;
        }
        return result;
    }

    public int hashCode() {
        int result = Integer.MIN_VALUE;
        result += (cmrField == null ? 0 : cmrField.hashCode());
        result += (eagerLoadGroup == null ? 0 : eagerLoadGroup.hashCode());
        result += (leftJoinList == null ? 0 : leftJoinList.hashCode());
        return result;
    }

    public String toString() {
        return "[cmr-field=" + cmrField + ", eager-load-group=" + eagerLoadGroup + ", left-join=" + leftJoinList + ']';
    }

    public void setCmrField(final String cmrField) {
        this.cmrField = cmrField;
    }

    public void setEagerLoadGroup(final String eagerLoadGroup) {
        this.eagerLoadGroup = eagerLoadGroup;
    }

    public void setLeftJoins(List<JDBCLeftJoinMetaData> leftJoins) {
        this.leftJoinList = leftJoins;
    }
}
