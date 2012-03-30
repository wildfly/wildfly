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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.jboss.as.cli.gui.CliGuiContext;

/**
 * This component produces a JPanel containing a sortable table that allows choosing
 * a deployment that exists on the server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class DeploymentChooser extends JPanel {

    private StandaloneDeploymentTableModel model;

    public DeploymentChooser(CliGuiContext cliGuiCtx, boolean isStandalone) {
        if (isStandalone) {
            model = new StandaloneDeploymentTableModel(cliGuiCtx);
        } else {
            model = new DomainDeploymentTableModel(cliGuiCtx);
        }

        DeploymentTable table = new DeploymentTable(model, isStandalone);
        JScrollPane scroller = new JScrollPane(table);
        add(scroller);
    }

    /**
     * Get the name of the selected deployment.
     * @return The name or null if there are no deployments.
     */
    public String getSelectedDeployment() {
        return model.getSelectedDeployment();
    }

    public boolean hasDeployments() {
        return model.hasDeployments();
    }

}
