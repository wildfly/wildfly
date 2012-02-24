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
package org.jboss.as.cmp.jdbc2.schema;

import java.sql.SQLException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMRFieldBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.tm.TransactionLocal;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 94413 $</tt>
 */
public class Schema {
    private final String viewsTxLocalKey;

    private EntityTable[] entityTables;
    private RelationTable[] relationTables;

    private TransactionLocal txLocal = null; // TODO: jeb - get this

    public Schema(String ejbModuleName) {
        this.viewsTxLocalKey = ejbModuleName + ".schema.views";
    }

    public EntityTable createEntityTable(JDBCEntityMetaData metadata, JDBCEntityBridge2 entity) {
        if (entityTables == null) {
            entityTables = new EntityTable[1];
        } else {
            EntityTable[] tmp = entityTables;
            entityTables = new EntityTable[tmp.length + 1];
            System.arraycopy(tmp, 0, entityTables, 0, tmp.length);
        }

        EntityTable table = new EntityTable(metadata, entity, this, entityTables.length - 1);
        entityTables[entityTables.length - 1] = table;
        return table;
    }

    public RelationTable createRelationTable(JDBCCMRFieldBridge2 leftField, JDBCCMRFieldBridge2 rightField) {
        if (relationTables == null) {
            relationTables = new RelationTable[1];
        } else {
            RelationTable[] tmp = relationTables;
            relationTables = new RelationTable[tmp.length + 1];
            System.arraycopy(tmp, 0, relationTables, 0, tmp.length);
        }

        RelationTable table = new RelationTable(leftField, rightField, this, relationTables.length - 1);
        relationTables[relationTables.length - 1] = table;
        return table;
    }

    public Table.View getView(EntityTable table) {
        Views views = getViews();
        Table.View view = views.entityViews[table.getTableId()];
        if (view == null) {
            view = table.createView(views.tx);
            views.entityViews[table.getTableId()] = view;
        }
        return view;
    }

    public Table.View getView(RelationTable table) {
        Views views = getViews();
        Table.View view = views.relationViews[table.getTableId()];
        if (view == null) {
            view = table.createView(views.tx);
            views.relationViews[table.getTableId()] = view;
        }
        return view;
    }

    public void flush() {
        Views views = getViews();

        Table.View[] relationViews = views.relationViews;
        if (relationViews != null) {
            for (int i = 0; i < relationViews.length; ++i) {
                final Table.View view = relationViews[i];
                if (view != null) {
                    try {
                        view.flushDeleted(views);
                    } catch (SQLException e) {
                        throw MESSAGES.failedToDeleteManyToMany(e);
                    }
                }
            }
        }

        final Table.View[] entityViews = views.entityViews;
        for (int i = 0; i < entityViews.length; ++i) {
            Table.View view = entityViews[i];
            if (view != null) {
                try {
                    view.flushDeleted(views);
                } catch (SQLException e) {
                    throw MESSAGES.failedToDeleteInstance(e);
                }
            }
        }

        for (int i = 0; i < entityViews.length; ++i) {
            Table.View view = entityViews[i];
            if (view != null) {
                try {
                    view.flushCreated(views);
                } catch (SQLException e) {
                    throw MESSAGES.failedToCreateInstance(e);
                }
            }
        }

        for (int i = 0; i < entityViews.length; ++i) {
            Table.View view = entityViews[i];
            if (view != null) {
                try {
                    view.flushUpdated();
                } catch (SQLException e) {
                    throw MESSAGES.failedToUpdateInstance(e);
                }
            }
        }

        if (relationViews != null) {
            for (int i = 0; i < relationViews.length; ++i) {
                final Table.View view = relationViews[i];
                if (view != null) {
                    try {
                        view.flushCreated(views);
                    } catch (SQLException e) {
                        throw MESSAGES.failedToCreateManyToMany(e);
                    }
                }
            }
        }
    }

    private Views getViews() {
        return null;
    }

    // Inner

    public class Views {
        public final Transaction tx;
        public final Table.View[] entityViews;
        public final Table.View[] relationViews;

        public Views(Transaction tx) {
            this.tx = tx;
            this.entityViews = new Table.View[entityTables.length];
            this.relationViews = relationTables == null ? null : new Table.View[relationTables.length];
        }
    }

    private class SchemaSynchronization implements Synchronization {
        private final Views views;

        public SchemaSynchronization(Views views) {
            this.views = views;
        }

        public void beforeCompletion() {
            flush();

            for (int i = 0; i < views.entityViews.length; ++i) {
                Table.View view = views.entityViews[i];
                if (view != null) {
                    view.beforeCompletion();
                }
            }
        }

        public void afterCompletion(int status) {
            if (status == Status.STATUS_MARKED_ROLLBACK ||
                    status == Status.STATUS_ROLLEDBACK ||
                    status == Status.STATUS_ROLLING_BACK) {
                for (int i = 0; i < views.entityViews.length; ++i) {
                    Table.View view = views.entityViews[i];
                    if (view != null) {
                        view.rolledback();
                    }
                }
            } else {
                for (int i = 0; i < views.entityViews.length; ++i) {
                    Table.View view = views.entityViews[i];
                    if (view != null) {
                        view.committed();
                    }
                }
            }
        }
    }
}
