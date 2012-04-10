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
package org.jboss.as.cli.gui.charts;
import javax.swing.JTabbedPane;
import org.jboss.as.cli.gui.ManagementModelNode;
import org.jboss.as.cli.gui.ManagementModelNode.UserObject;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jboss.as.cli.gui.CliGuiContext;

/**
 * Creates a dialog that lets you build a deploy command.  This dialog
 * behaves differently depending on standalone or domain mode.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CreateAChartDialog extends JDialog implements ActionListener {
    private CliGuiContext cliGuiCtx;
    private ManagementModelNode node;
    private JPanel inputPanel = new JPanel(new GridBagLayout());
    private JTextField graphNameField = new JTextField(20);
    private JTextField descriptionField = new JTextField(40);

    public CreateAChartDialog(CliGuiContext cliGuiCtx, ManagementModelNode node) {
        super(cliGuiCtx.getMainWindow(), "Real Time Graph", Dialog.ModalityType.APPLICATION_MODAL);
        this.cliGuiCtx = cliGuiCtx;
        this.node = node;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));

        contentPane.add(makeInputPanel(), BorderLayout.CENTER);

        contentPane.add(makeButtonPanel(), BorderLayout.SOUTH);
        pack();
        setResizable(false);
    }

    private JPanel makeInputPanel() {
        UserObject usrObj = (UserObject)node.getUserObject();
        String name = usrObj.getName();
        graphNameField.setText(name);
        descriptionField.setText(usrObj.getAttributeProps().getDescription());

        GridBagConstraints gbConst = new GridBagConstraints();
        gbConst.anchor = GridBagConstraints.WEST;
        gbConst.insets = new Insets(5,5,5,5);

        JLabel headingLabel = new JLabel("<html><b>Set up graph for " + name + ".</b></html>");
        gbConst.gridwidth = GridBagConstraints.REMAINDER;
        inputPanel.add(headingLabel, gbConst);

        JLabel graphNameLabel = new JLabel("Graph Name:");
        graphNameLabel.setToolTipText("The title of your graph.");
        gbConst.gridwidth = 1;
        inputPanel.add(graphNameLabel, gbConst);

        addStrut();
        gbConst.gridwidth = GridBagConstraints.REMAINDER;
        inputPanel.add(graphNameField, gbConst);

        JLabel descriptionLabel = new JLabel("Description:");
        descriptionLabel.setToolTipText("The description of the attribute you are graphing.");
        gbConst.gridwidth = 1;
        inputPanel.add(descriptionLabel, gbConst);

        addStrut();
        gbConst.gridwidth = GridBagConstraints.REMAINDER;
        inputPanel.add(descriptionField, gbConst);

        return inputPanel;
    }

    private void addStrut() {
        inputPanel.add(Box.createHorizontalStrut(5));
    }

    private JPanel makeButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        ok.setMnemonic(KeyEvent.VK_ENTER);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                CreateAChartDialog.this.dispose();
            }
        });

        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        return buttonPanel;
    }

    public void actionPerformed(ActionEvent e) {
        JBossChart chart = new JBossChart(cliGuiCtx, graphNameField.getText(), descriptionField.getText(), node);
        cliGuiCtx.getChartManager().addChart(chart);
        dispose();
    }

}
