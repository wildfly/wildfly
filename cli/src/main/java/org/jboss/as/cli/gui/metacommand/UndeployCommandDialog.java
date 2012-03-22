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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jboss.as.cli.gui.GuiMain;
import org.jboss.as.cli.gui.component.DeploymentChooser;
import org.jboss.as.cli.gui.component.HelpButton;
import org.jboss.as.cli.gui.component.ServerGroupChooser;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class UndeployCommandDialog extends JDialog implements ActionListener {

    private DeploymentChooser deploymentChooser = new DeploymentChooser();
    private ServerGroupChooser serverGroupChooser = new ServerGroupChooser();

    public UndeployCommandDialog() {
        super(GuiMain.getMainWindow(), "undeploy", Dialog.ModalityType.APPLICATION_MODAL);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));

        contentPane.add(makeInputPanel(), BorderLayout.CENTER);

        contentPane.add(makeButtonPanel(), BorderLayout.SOUTH);
        pack();
        setResizable(false);
    }

    private JPanel makeInputPanel() {
        JPanel inputPanel = new JPanel(new GridLayout(4, 1));

        if (deploymentChooser.hasDeployments()) {
            inputPanel.add(deploymentChooser);
        } else {
            inputPanel.add(new JLabel("NO DEPLOYMENTS AVAILABLE TO UNDEPLOY"));
        }

        if (!serverGroupChooser.isStandalone()) {
            inputPanel.add(serverGroupChooser);
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
    /*    StringBuilder builder = new StringBuilder("deploy");

        String path = pathField.getText();
        if (!path.trim().isEmpty()) builder.append("  ").append(path);

        String name = nameField.getText();
        if (!name.trim().isEmpty()) builder.append("  --name=").append(name);

        String runtimeName = runtimeNameField.getText();
        if (!runtimeName.trim().isEmpty()) builder.append("  --runtime_name=").append(runtimeName);

        if (forceCheckBox.isSelected()) builder.append("  --force");
        if (disabledCheckBox.isSelected()) builder.append("  --disabled");

        if (!serverGroupChooser.isStandalone()) {
            if (serverGroupChooser.allServerGroupsChecked()) {
                builder.append("  --all-server-groups");
            } else {
                builder.append("  --server-groups=");
                for (JCheckBox serverGroup : serverGroupChooser.getServerGroups()) {
                    if (serverGroup.isSelected()) {
                        builder.append(serverGroup.getText());
                        builder.append(",");
                    }
                }
                builder.deleteCharAt(builder.length() - 1); // remove trailing comma
            }
        }

        JTextComponent cmdText = GuiMain.getCommandLine().getCmdText();
        cmdText.setText(builder.toString());
        dispose();
        cmdText.requestFocus(); */
    }

}
