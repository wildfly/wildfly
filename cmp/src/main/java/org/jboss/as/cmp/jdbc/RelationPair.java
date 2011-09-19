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
package org.jboss.as.cmp.jdbc;

import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;

/**
 * This class represents one pair of entities in a relation.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class RelationPair {
    private final JDBCCMRFieldBridge leftCMRField;
    private final JDBCCMRFieldBridge rightCMRField;

    private final Object leftId;
    private final Object rightId;

    public RelationPair(
            JDBCCMRFieldBridge leftCMRField, Object leftId,
            JDBCCMRFieldBridge rightCMRField, Object rightId) {

        this.leftCMRField = leftCMRField;
        this.leftId = leftId;

        this.rightCMRField = rightCMRField;
        this.rightId = rightId;
    }

    public Object getLeftId() {
        return leftId;
    }

    public Object getRightId() {
        return rightId;
    }

    public boolean equals(Object obj) {
        if (obj instanceof RelationPair) {
            RelationPair pair = (RelationPair) obj;

            // check left==left and right==right
            if (leftCMRField == pair.leftCMRField &&
                    rightCMRField == pair.rightCMRField &&
                    leftId.equals(pair.leftId) &&
                    rightId.equals(pair.rightId)) {
                return true;
            }

            // check left==right and right==left
            if (leftCMRField == pair.rightCMRField &&
                    rightCMRField == pair.leftCMRField &&
                    leftId.equals(pair.rightId) &&
                    rightId.equals(pair.leftId)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return leftId.hashCode() ^ rightId.hashCode();
    }
}

