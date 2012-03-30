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

import java.util.ArrayList;
import java.util.List;
import javax.swing.JRadioButton;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.dmr.ModelNode;

/**
 * A table model appropriate for deployments in a domain.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class DomainDeploymentTableModel extends StandaloneDeploymentTableModel {

    private CliGuiContext cliGuiCtx;

    public DomainDeploymentTableModel(CliGuiContext cliGuiCtx) {
        super(cliGuiCtx);
        this.cliGuiCtx = cliGuiCtx;
        colNames = new String[] {"Name", "Runtime Name", "Assigned Server Groups"};
        initializeServerGroups();
        setServerGroups();
    }

    private void initializeServerGroups() {
        for (Object[] deployment : data) {
            deployment[2] = new ArrayList<String>();
        }
    }

    private void setServerGroups() {
        ModelNode deploymentsQuery = null;
        String queryString = "/server-group=*/deployment=*/:read-resource";

        try {
            deploymentsQuery = cliGuiCtx.getExecutor().doCommand(queryString);
            if (deploymentsQuery.get("outcome").asString().equals("failed")) return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (ModelNode node : deploymentsQuery.get("result").asList()) {
            String serverGroup = node.get("address").asPropertyList().get(0).getValue().asString();
            ModelNode deploymentNode = node.get("result"); // get the inner result

            Object[] deployment = findDeployment(deploymentNode.get("name").asString());

            List<String> serverGroups = (List<String>)deployment[2];

            Boolean enabled = deploymentNode.get("enabled").asBoolean();
            if (!enabled) serverGroup += " (disabled)";
            serverGroups.add(serverGroup);
        }
    }

    private Object[] findDeployment(String name) {
        for (Object[] deployment : data) {
            JRadioButton nameButton = (JRadioButton)deployment[0];
            if (nameButton.getText().equals(name)) return deployment;
        }

        throw new IllegalStateException("Deployment " + name + " exists in server group but not in content repository.");
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return JRadioButton.class;
        if (columnIndex == 2) return List.class;
        return String.class;
    }
}
