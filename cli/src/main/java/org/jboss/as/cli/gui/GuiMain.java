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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.ToolTipManager;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.gui.metacommand.DeployAction;

/**
 * Static main class for the GUI.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class GuiMain {
    private static JFrame frame;

    private static CommandExecutor executor;

    private static Container contentPane;
    private static JPanel mainPanel = new JPanel();

    private static CommandLine cmdLine;
    private static JTextPane output = new JTextPane();
    private static JTabbedPane tabs;
    private static DoOperationActionListener opListener;

    private GuiMain() {} // don't allow an instance

    public static synchronized void start(CommandContext cmdCtx) {
        if (executor != null) throw new RuntimeException("Gui is already initialized.");
        executor = new CommandExecutor(cmdCtx);
        ToolTipManager.sharedInstance().setDismissDelay(15000);
        initJFrame();
    }

    /**
     * Get the singleton JFrame instance for the GUI
     * @return The JFrame
     */
    public static JFrame getFrame() {
        return frame;
    }

    /**
     * Get the main command line.
     * @return The main command text field.
     */
    public static CommandLine getCommandLine() {
        return cmdLine;
    }

    /**
     * Get the command executor.
     * @return The command executor.
     */
    public static CommandExecutor getExecutor() {
        return executor;
    }

    private static synchronized void initJFrame() {
        frame = new JFrame("CLI GUI");
        URL iconURL = GuiMain.class.getResource("/icon/as7_logo.png");
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(iconURL));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setJMenuBar(makeMenuBar());
        frame.setSize(800, 600);

        contentPane = frame.getContentPane();
        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        contentPane.add(mainPanel, BorderLayout.CENTER);

        mainPanel.setLayout(new BorderLayout(5,5));
        tabs = makeTabbedPane();

        opListener = new DoOperationActionListener(output, tabs);
        cmdLine = new CommandLine(opListener);

        output.addMouseListener(new SelectPreviousOpMouseAdapter(output, opListener));

        mainPanel.add(cmdLine, BorderLayout.NORTH);
        mainPanel.add(tabs, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private static JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu metaCmdMenu = new JMenu("Meta Commands");
        metaCmdMenu.setMnemonic(KeyEvent.VK_M);
        menuBar.add(metaCmdMenu);

        JMenuItem deploy = new JMenuItem(new DeployAction());
        deploy.setMnemonic(KeyEvent.VK_D);
        metaCmdMenu.add(deploy);

        return menuBar;
    }

    private static JTabbedPane makeTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Command Builder", new JScrollPane(new ManagementModel()));
        tabs.addTab("Output", makeOutputDisplay());
        return tabs;
    }

    private static JPanel makeOutputDisplay() {
        JPanel outputDisplay = new JPanel();
        outputDisplay.setSize(400, 5000);
        outputDisplay.setLayout(new BorderLayout(5,5));
        output.setEditable(false);
        outputDisplay.add(new JScrollPane(output), BorderLayout.CENTER);
        return outputDisplay;
    }
}
