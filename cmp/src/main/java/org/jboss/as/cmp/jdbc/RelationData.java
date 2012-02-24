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

import java.util.HashSet;
import java.util.Set;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;

/**
 * This class holds data about one relationship. It maintains a lists of
 * which relations have been added and removed. When the transaction is
 * committed these list are retrieved and used to update the relation table.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class RelationData {
    private final JDBCCMRFieldBridge leftCMRField;
    private final JDBCCMRFieldBridge rightCMRField;

    public final Set addedRelations = new HashSet();
    public final Set removedRelations = new HashSet();
    public final Set notRelatedPairs = new HashSet();

    public RelationData(JDBCCMRFieldBridge leftCMRField, JDBCCMRFieldBridge rightCMRField) {

        this.leftCMRField = leftCMRField;
        this.rightCMRField = rightCMRField;
    }

    public JDBCCMRFieldBridge getLeftCMRField() {
        return leftCMRField;
    }

    public JDBCCMRFieldBridge getRightCMRField() {
        return rightCMRField;
    }

    public void addRelation(JDBCCMRFieldBridge leftCMRField,
                            Object leftId,
                            JDBCCMRFieldBridge rightCMRField,
                            Object rightId) {
        // only need to bother if neither side has a foreign key
        if (!leftCMRField.hasForeignKey() && !rightCMRField.hasForeignKey()) {
            RelationPair pair = createRelationPair(leftCMRField, leftId, rightCMRField, rightId);
            if (removedRelations.contains(pair)) {
                // we were going to remove this relation
                // and now we are adding it.  Just
                // remove it from the remove set and we are ok.
                removedRelations.remove(pair);
            } else {
                addedRelations.add(pair);

                // if pair was specifically marked as
                // not related, remove it to the not
                // related set.  See below.
                if (notRelatedPairs.contains(pair)) {
                    notRelatedPairs.remove(pair);
                }
            }
        }
    }

    public void removeRelation(JDBCCMRFieldBridge leftCMRField,
                               Object leftId,
                               JDBCCMRFieldBridge rightCMRField,
                               Object rightId) {
        // only need to bother if neither side has a foreign key
        if (!leftCMRField.hasForeignKey() && !rightCMRField.hasForeignKey()) {
            RelationPair pair = createRelationPair(leftCMRField, leftId, rightCMRField, rightId);
            if (addedRelations.contains(pair)) {
                // we were going to add this relation
                // and now we are removing it.  Just
                // remove it from the add set and we are ok.
                addedRelations.remove(pair);

                // add it to the set of not related pairs
                // so if remove is called again it is not
                // added to the remove list. This avoids
                // an extra 'DELETE FROM...' query.
                // This happend when a object is moved from
                // one relation to another. See
                // JDBCCMRFieldBridge.createRelationLinks
                notRelatedPairs.add(pair);
            } else {
                // if pair is related (not not related)
                // add it to the remove set.  See above.
                if (!notRelatedPairs.contains(pair)) {
                    removedRelations.add(pair);
                }
            }
        }
    }

    public boolean isDirty() {
        return addedRelations.size() > 0 || removedRelations.size() > 0;
    }

    private RelationPair createRelationPair(JDBCCMRFieldBridge leftCMRField,
                                            Object leftId,
                                            JDBCCMRFieldBridge rightCMRField,
                                            Object rightId) {
        if (this.leftCMRField == leftCMRField && this.rightCMRField == rightCMRField) {
            return new RelationPair(leftCMRField, leftId, rightCMRField, rightId);
        }

        if (this.leftCMRField == rightCMRField && this.rightCMRField == leftCMRField) {
            return new RelationPair(rightCMRField, rightId, leftCMRField, leftId);
        }
        throw MESSAGES.cmrFieldsWrongType();
    }
}


