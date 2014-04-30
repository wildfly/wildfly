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

package org.jboss.as.cli.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.gui.component.ServerLogsTable;
import org.jboss.as.cli.gui.component.ServerLogsTableModel;
import org.jboss.as.cli.gui.metacommand.DownloadServerLogDialog;
import org.jboss.dmr.ModelNode;

/**
 * The main panel for listing the server logs.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class ServerLogsPanel extends JPanel {

    private ServerLogsTable table;
    private ServerLogsTableModel tableModel;
    private CliGuiContext cliGuiCtx;
    private ManagementModelNode loggingSubsys;

    public ServerLogsPanel(CliGuiContext cliGuiCtx, ManagementModelNode loggingSubsys) {
        this.cliGuiCtx = cliGuiCtx;
        this.loggingSubsys = loggingSubsys;

        setLayout(new BorderLayout());
        this.table = new ServerLogsTable();
        this.tableModel = new ServerLogsTableModel(cliGuiCtx, this.table);
        this.table.setModel(tableModel);
        this.tableModel.refresh();

        JScrollPane scroller = new JScrollPane(table);
        add(scroller, BorderLayout.CENTER);
        add(makeButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel makeButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new ViewWithCLIButton());
        buttonPanel.add(new DownloadButton());
        buttonPanel.add(new RefreshButton());
        return buttonPanel;
    }

    private String getSelectedFileName() {
        return (String)table.getValueAt(table.getSelectedRow(), 0);
    }

    private Long getSelectedFileSize() {
        return (Long)table.getValueAt(table.getSelectedRow(), 2);
    }

    private class ViewWithCLIButton extends JButton {
        private String description;
        private ModelNode requestProperties;

        public ViewWithCLIButton() {
            super("View with CLI");
            retrieveOpDescription();

            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    OperationDialog opDialog = new OperationDialog(cliGuiCtx, loggingSubsys, "read-log-file", description, requestProperties);
                    opDialog.setValue("name", getSelectedFileName());
                    opDialog.setLocationRelativeTo(cliGuiCtx.getMainWindow());
                    opDialog.setVisible(true);
                }
            });
        }

        private void retrieveOpDescription() {
            try {
                ModelNode result = cliGuiCtx.getExecutor().doCommand("/subsystem=logging/:read-operation-description(name=read-log-file)");
                this.description = result.get("result", "description").asString();
                this.requestProperties = result.get("result", "request-properties");
            } catch (IOException | CommandFormatException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class DownloadButton extends JButton {
        public DownloadButton() {
            super("Download");
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DownloadServerLogDialog dialog = new DownloadServerLogDialog(cliGuiCtx, getSelectedFileName(), getSelectedFileSize());
                    dialog.setLocationRelativeTo(cliGuiCtx.getMainWindow());
                    dialog.setVisible(true);
                }
            });
        }
    }

    private class RefreshButton extends JButton {
        public RefreshButton() {
            super("Refresh List");
            addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    tableModel.refresh();
                }
            });
        }
    }
}
