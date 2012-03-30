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
package org.jboss.as.cli.gui.metacommand;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.as.cli.gui.component.DeploymentChooser;
import org.jboss.as.cli.gui.component.HelpButton;
import org.jboss.as.cli.gui.component.ServerGroupChooser;

/**
 * Dialog for creating an undeploy command.  This dialog behaves differently for
 * standalone or domain mode.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class UndeployCommandDialog extends JDialog implements ActionListener {

    private CliGuiContext cliGuiCtx;
    private ServerGroupChooser serverGroupChooser;
    private DeploymentChooser deploymentChooser;

    private JCheckBox keepContent = new JCheckBox("Keep Content");
    private JCheckBox allRelevantServerGroups = new JCheckBox("All RelevantServer Groups");

    public UndeployCommandDialog(CliGuiContext cliGuiCtx) {
        super(cliGuiCtx.getMainWindow(), "undeploy", Dialog.ModalityType.APPLICATION_MODAL);
        this.cliGuiCtx = cliGuiCtx;
        this.serverGroupChooser = new ServerGroupChooser(cliGuiCtx);
        this.deploymentChooser = new DeploymentChooser(cliGuiCtx, serverGroupChooser.isStandalone());

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));

        contentPane.add(makeInputPanel(), BorderLayout.CENTER);
        setRelevantServerGroupsListener();

        contentPane.add(makeButtonPanel(), BorderLayout.SOUTH);
        pack();
        setResizable(false);
    }

    private void setRelevantServerGroupsListener() {
        allRelevantServerGroups.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                serverGroupChooser.setEnabled(!allRelevantServerGroups.isSelected());
            }
        });
    }

    private JPanel makeInputPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        if (!deploymentChooser.hasDeployments()) {
            inputPanel.add(new JLabel("NO DEPLOYMENTS AVAILABLE TO UNDEPLOY"), gbc);
            return inputPanel;
        }

        inputPanel.add(deploymentChooser, gbc);
        inputPanel.add(keepContent, gbc);

        if (!serverGroupChooser.isStandalone()) {
            inputPanel.add(Box.createVerticalStrut(30), gbc);
            inputPanel.add(serverGroupChooser, gbc);
            inputPanel.add(allRelevantServerGroups, gbc);
        }

        return inputPanel;
    }

    private JPanel makeButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        ok.setMnemonic(KeyEvent.VK_ENTER);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                UndeployCommandDialog.this.dispose();
            }
        });

        if (deploymentChooser.hasDeployments()) {
            buttonPanel.add(ok);
        }

        buttonPanel.add(cancel);
        buttonPanel.add(new HelpButton("undeploy.txt"));
        return buttonPanel;
    }

    public void actionPerformed(ActionEvent e) {
        StringBuilder builder = new StringBuilder("undeploy  ");

        String name = deploymentChooser.getSelectedDeployment();
        builder.append(name);

        if (keepContent.isSelected()) builder.append("  --keep-content");

        if (!serverGroupChooser.isStandalone()) {
            addDomainParams(builder);
        }

        JTextComponent cmdText = cliGuiCtx.getCommandLine().getCmdText();
        cmdText.setText(builder.toString());
        dispose();
        cmdText.requestFocus();
    }

    private void addDomainParams(StringBuilder builder) {
        if (!allRelevantServerGroups.isSelected()) {
            builder.append(serverGroupChooser.getCmdLineArg());
        } else {
            builder.append("  --all-relevant-server-groups");
        }
    }

}
