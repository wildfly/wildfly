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
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.gui.metacommand.DeployAction;
import org.jboss.as.cli.gui.metacommand.UndeployAction;

/**
 * Static main class for the GUI.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class GuiMain {
    private static Image jbossIcon;
    static {
        URL iconURL = GuiMain.class.getResource("/icon/as7_logo.png");
        jbossIcon = Toolkit.getDefaultToolkit().getImage(iconURL);
        ToolTipManager.sharedInstance().setDismissDelay(15000);
    }

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(GuiMain.class);
    private static String LOOK_AND_FEEL_KEY = "cli-gui-laf";

    private GuiMain() {} // don't allow an instance

    public static void start(CommandContext cmdCtx) {
        initJFrame(makeGuiContext(cmdCtx));
    }

    public static CliGuiContext startEmbedded(CommandContext cmdCtx) {
        return makeGuiContext(cmdCtx);
    }

    private static CliGuiContext makeGuiContext(CommandContext cmdCtx) {
        CliGuiContext cliGuiCtx = new CliGuiContext();
        CommandExecutor executor = new CommandExecutor(cliGuiCtx, cmdCtx);
        cliGuiCtx.setExecutor(executor);

        JTextPane output = new JTextPane();
        JPanel outputDisplay = makeOutputDisplay(output);
        JTabbedPane tabs = makeTabbedPane(cliGuiCtx, outputDisplay);

        DoOperationActionListener opListener = new DoOperationActionListener(cliGuiCtx, output, tabs);
        CommandLine cmdLine = new CommandLine(opListener);
        cliGuiCtx.setCommandLine(cmdLine);

        output.addMouseListener(new SelectPreviousOpMouseAdapter(cliGuiCtx, output, opListener));

        JPanel mainPanel = makeMainPanel(tabs, cmdLine);
        cliGuiCtx.setMainPanel(mainPanel);

        return cliGuiCtx;
    }

    private static JPanel makeMainPanel(JTabbedPane tabs, CommandLine cmdLine) {
        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        mainPanel.setLayout(new BorderLayout(5,5));
        mainPanel.add(cmdLine, BorderLayout.NORTH);
        mainPanel.add(tabs, BorderLayout.CENTER);
        return mainPanel;
    }

    public static Image getJBossIcon() {
        return jbossIcon;
    }

    private static synchronized void initJFrame(CliGuiContext cliGuiCtx) {
        setUpLookAndFeel();
        JFrame frame = new JFrame("CLI GUI");

        frame.setIconImage(getJBossIcon());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setJMenuBar(makeMenuBar(cliGuiCtx));
        frame.setSize(800, 600);

        Container contentPane = frame.getContentPane();

        contentPane.add(cliGuiCtx.getMainPanel(), BorderLayout.CENTER);

        frame.setVisible(true);
    }

    public static JMenuBar makeMenuBar(CliGuiContext cliGuiCtx) {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(makeMetaCmdMenu(cliGuiCtx));
        return menuBar;
    }

    private static JMenu makeMetaCmdMenu(CliGuiContext cliGuiCtx) {
        JMenu metaCmdMenu = new JMenu("Meta Commands");
        metaCmdMenu.setMnemonic(KeyEvent.VK_M);


        JMenuItem deploy = new JMenuItem(new DeployAction(cliGuiCtx));
        deploy.setMnemonic(KeyEvent.VK_D);
        metaCmdMenu.add(deploy);

        JMenuItem unDeploy = new JMenuItem(new UndeployAction(cliGuiCtx));
        deploy.setMnemonic(KeyEvent.VK_U);
        metaCmdMenu.add(unDeploy);

        // Add look & feel options
        final LookAndFeelInfo[] all = UIManager.getInstalledLookAndFeels();
        if (all != null) {
            final String errorTitle = "Look & Feel Not Set";
            final JMenu lfMenu = new JMenu("Look & Feel");
            lfMenu.setMnemonic(KeyEvent.VK_L);
            menuBar.add(lfMenu);

            for (final LookAndFeelInfo lookAndFeelInfo : all) {
                JMenuItem item = new JMenuItem(new AbstractAction(lookAndFeelInfo.getName()) {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        try {
                            UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                            SwingUtilities.updateComponentTreeUI(frame);
                            PREFERENCES.put(LOOK_AND_FEEL_KEY, lookAndFeelInfo.getClassName());
                        } catch (ClassNotFoundException e1) {
                            showErrorDialog(errorTitle, e1);
                        } catch (InstantiationException e1) {
                            showErrorDialog(errorTitle, e1);
                        } catch (IllegalAccessException e1) {
                            showErrorDialog(errorTitle, e1);
                        } catch (UnsupportedLookAndFeelException e1) {
                            showErrorDialog(errorTitle, e1);
                        }
                    }
                });
                lfMenu.add(item);
            }
        }


        return menuBar;
    }

    private static JTabbedPane makeTabbedPane(CliGuiContext cliGuiCtx, JPanel output) {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Command Builder", new JScrollPane(new ManagementModel(cliGuiCtx)));
        tabs.addTab("Output", output);
        return tabs;
    }

    private static JPanel makeOutputDisplay(JTextPane output) {
        JPanel outputDisplay = new JPanel();
        outputDisplay.setSize(400, 5000);
        outputDisplay.setLayout(new BorderLayout(5,5));
        output.setEditable(false);
        outputDisplay.add(new JScrollPane(output), BorderLayout.CENTER);
        return outputDisplay;
    }

    private static void setUpLookAndFeel() {
        try {
            final String laf = PREFERENCES.get(LOOK_AND_FEEL_KEY, UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel(laf);
        } catch (Throwable e) {
            // Just ignore if the L&F has any errors
        }
    }

    private static void showErrorDialog(final String title, final Throwable t) {
        JOptionPane.showMessageDialog(frame, t.getLocalizedMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
