/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

import java.awt.Window;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.jboss.as.cli.gui.charts.ChartManager;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CliGuiContext {

    private boolean isEmbedded = false;

    private CommandExecutor executor;
    private JPanel mainPanel;
    private JTabbedPane tabs;
    private CommandLine cmdLine;
    private boolean isStandalone;
    private ChartManager chartManager = new ChartManager(this);

    CliGuiContext(boolean isEmbedded) {
        this.isEmbedded = isEmbedded;
    }

    void setExecutor(CommandExecutor executor) {
        this.executor = executor;
        try {
            ModelNode result = executor.doCommand("/:read-attribute(name=process-type)");
            this.isStandalone = result.get("result").asString().equals("Server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setMainPanel(JPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    void setTabs(JTabbedPane tabs) {
        this.tabs = tabs;
    }

    void setCommandLine(CommandLine cmdLine) {
        this.cmdLine = cmdLine;
    }

    /**
     * Find if CLI GUI is running in its own JFrame (kicked off by jboss-cli) or
     * embedded in another JFrame (kicked off by jconsole).
     * @return true if embedded, false otherwise.
     */
    public boolean isEmbedded() {
        return this.isEmbedded;
    }
    /**
     * Find if we are connected to a standalone AS instances
     * or a domain controller.
     * @return true if standalone, false otherwise.
     */
    public boolean isStandalone() {
        return this.isStandalone;
    }

    /**
     * Get the main panel for CLI GUI.
     * @return The main panel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Get the tabs for CLI GUI.
     * @return The tabs.
     */
    public JTabbedPane getTabs() {
        return this.tabs;
    }

    /**
     * Get the singleton JFrame instance for the GUI
     * @return The JFrame
     */
    public Window getMainWindow() {
        return SwingUtilities.getWindowAncestor(mainPanel);
    }

    /**
     * Get the main command line.
     * @return The main command text field.
     */
    public CommandLine getCommandLine() {
        return cmdLine;
    }

    /**
     * Get the command executor.
     * @return The command executor.
     */
    public CommandExecutor getExecutor() {
        return executor;
    }

    public ChartManager getChartManager() {
        return this.chartManager;
    }
}
