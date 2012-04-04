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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CommandLine extends JPanel {
    private static final String SUBMIT_ACTION = "submit-action";

    private JTextArea cmdText = new JTextArea();
    private JCheckBox isVerbose = new JCheckBox("Verbose");
    private JButton submitButton = new JButton("Submit");

    public CommandLine(DoOperationActionListener opListener) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0,0,0,5);
        add(new JLabel("cmd>"), gbc);

        cmdText.setBorder(new LineBorder(Color.BLACK));
        cmdText.setText("/");
        cmdText.setLineWrap(true);
        cmdText.setRows(1);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 100.0;
        add(cmdText, gbc);

        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
        cmdText.getInputMap().put(enterKey, SUBMIT_ACTION);
        cmdText.getActionMap().put(SUBMIT_ACTION, opListener);

        JPanel submitPanel = new JPanel(new GridLayout(2,1));
        submitButton.addActionListener(opListener);
        submitButton.setToolTipText("Submit the command to the server.");
        submitPanel.add(submitButton);

        isVerbose.setToolTipText("Show the command's DMR request.");
        submitPanel.add(isVerbose);

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.weightx = 1.0;
        add(submitPanel, gbc);
    }

    public boolean isVerbose() {
        return isVerbose.isSelected();
    }

    public JTextComponent getCmdText() {
        return this.cmdText;
    }
}
