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
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import org.jboss.as.cli.CommandContext;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class GuiMain {
    private static final String SUBMIT_ACTION = "submit-action";

    private CommandContext cmdCtx;
    private CommandExecutor executor;

    private JFrame frame = new JFrame("CLI GUI");
    private Container contentPane;
    private JPanel mainPanel = new JPanel();
    private JTextField cmdText = new JTextField();
    private JButton submitButton = new JButton("Submit");
    private JTextPane output = new JTextPane();

    public GuiMain(CommandContext cmdCtx) {
        this.cmdCtx = cmdCtx;
        this.executor = new CommandExecutor(cmdCtx);
        initJFrame();
    }

    private void initJFrame() {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setSize(640, 480);

        contentPane = frame.getContentPane();
        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        contentPane.add(mainPanel, BorderLayout.CENTER);

        mainPanel.setLayout(new BorderLayout(5,5));
        mainPanel.add(makeCommandLine(), BorderLayout.NORTH);
        mainPanel.add(makeTabbedPane(), BorderLayout.CENTER);

        //frame.pack();
        frame.setVisible(true);
    }

    private JTabbedPane makeTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Management Model", new JScrollPane(new ManagementModel(cmdText, executor)));
        tabs.addTab("Output", makeOutputDisplay());
        return tabs;
    }

    private JPanel makeCommandLine() {
        JPanel cmdLine = new JPanel();
        cmdLine.setLayout(new BorderLayout(2,5));
        cmdLine.add(new JLabel("cmd>"), BorderLayout.WEST);
        cmdText.setText("/subsystem=weld/:read-resource");
        cmdLine.add(cmdText, BorderLayout.CENTER);
        Action submitListener = new DoOperationActionListener(executor, cmdText, output);

        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true);
        cmdText.getInputMap().put(enterKey, SUBMIT_ACTION);
        cmdText.getActionMap().put(SUBMIT_ACTION, submitListener);

        submitButton.addActionListener(submitListener);
        cmdLine.add(submitButton, BorderLayout.EAST);
        return cmdLine;
    }

    private JPanel makeOutputDisplay() {
        JPanel outputDisplay = new JPanel();
        outputDisplay.setSize(400, 5000);
        outputDisplay.setLayout(new BorderLayout(5,5));
        output.setEditable(false);
        outputDisplay.add(new JScrollPane(output), BorderLayout.CENTER);
        return outputDisplay;
    }
}
