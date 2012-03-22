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
package org.jboss.as.cli.gui.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import org.jboss.as.cli.gui.GuiMain;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class DeploymentChooser extends JPanel {

    private ButtonGroup deploymentsButtonGroup = new ButtonGroup();
    private JPanel deploymentsPanel = new JPanel(new FlowLayout());

    public DeploymentChooser() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Deployments"));
        setDeployments();
        add(deploymentsPanel, BorderLayout.CENTER);
    }

    private void setDeployments() {
        Set<String> deploymentNames = new TreeSet<String>();
        try {
            ModelNode deploymentsQuery = GuiMain.getExecutor().doCommand("/:read-children-names(child-type=deployment)");
            if (deploymentsQuery.get("outcome").asString().equals("failed")) return;

            for (ModelNode node : deploymentsQuery.get("result").asList()) {
                deploymentNames.add(node.asString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String name : deploymentNames) {
            JRadioButton deploymentRadioButton = new JRadioButton(name);
            deploymentsButtonGroup.add(deploymentRadioButton);

            // set first as selected
            if (deploymentsButtonGroup.getButtonCount() == 1) {
                deploymentsButtonGroup.setSelected(deploymentRadioButton.getModel(), true);
            }

            deploymentsPanel.add(deploymentRadioButton);
        }

    }

    /**
     * Get the name of the selected deployment.
     * @return The name or null if there are no deployments.
     */
    public String getSelectedDeployment() {
        if (!hasDeployments()) return null;

        ButtonModel model = deploymentsButtonGroup.getSelection();
        Object[] selected = model.getSelectedObjects();
        return ((JRadioButton)selected[0]).getText();
    }

    public boolean hasDeployments() {
        return deploymentsButtonGroup.getButtonCount() > 0;
    }

}
