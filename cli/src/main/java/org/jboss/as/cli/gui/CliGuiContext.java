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
import javax.swing.SwingUtilities;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CliGuiContext {

    private CommandExecutor executor;
    private JPanel mainPanel;
    private CommandLine cmdLine;

    CliGuiContext() {
    }

    void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    void setMainPanel(JPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    void setCommandLine(CommandLine cmdLine) {
        this.cmdLine = cmdLine;
    }

    /**
     * Get the main panel for CLI GUI.
     * @return The main panel
     */
    public JPanel getMainPanel() {
        return mainPanel;
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
}
