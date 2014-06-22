/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.cli.gui.component;

import java.io.IOException;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.dmr.ModelNode;

/**
 * The TableModel for the server logs.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class ServerLogsTableModel extends AbstractTableModel {
    private CliGuiContext cliGuiCtx;
    private List<ModelNode> allLogs;
    private ServerLogsTable table;

    protected String[] colNames = new String[] {"File", "Last Modified", "Size"};

    public ServerLogsTableModel(CliGuiContext cliGuiCtx, ServerLogsTable table) {
        this.cliGuiCtx = cliGuiCtx;
        this.table = table;
    }

    public void refresh() {
        try {
            this.allLogs = cliGuiCtx.getExecutor().doCommand("/subsystem=logging/:list-log-files").get("result").asList();
            fireTableDataChanged();
            table.getRowSorter().toggleSortOrder(1); // sort by last modified date
            table.getRowSorter().toggleSortOrder(1); // newest on top
            if (allLogs.size() > 0) table.setRowSelectionInterval(0, 0);
        } catch (IOException | CommandFormatException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getRowCount() {
        if (allLogs == null) return 0;
        return allLogs.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ModelNode row = allLogs.get(rowIndex);
        if (columnIndex == 0) return row.get("file-name").asString();
        if (columnIndex == 1) {
            String dateTime = row.get("last-modified-date").asString();
            dateTime = dateTime.substring(0, dateTime.indexOf('.'));
            String date = dateTime.substring(0, dateTime.indexOf('T'));
            String time = dateTime.substring(date.length() + 1, dateTime.length());
            return date + " " + time;
        }

        // column 2
        return row.get("file-size").asLong();
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 2) return Long.class;
        return String.class;
    }

}
