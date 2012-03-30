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
import java.util.Enumeration;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.table.AbstractTableModel;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.dmr.ModelNode;

/**
 * A table model appropriate for standalone mode.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class StandaloneDeploymentTableModel extends AbstractTableModel {

    private ButtonGroup deploymentsButtonGroup = new ButtonGroup();

    protected String[] colNames = new String[] {"Name", "Runtime Name", "Enabled"};
    protected List<Object[]> data = new ArrayList<Object[]>();

    public StandaloneDeploymentTableModel(CliGuiContext cliGuiCtx) {
        // for testing the table with lots of deployments
  /*    for (int i=0; i < 100; i++) {
          Object[] obj = new Object[3];
          JRadioButton button = new JRadioButton("fooname");
          deploymentsButtonGroup.add(button);
          obj[0] =  button;
          obj[1] = "foorulkja;dsfkja;ldskjfqpoeirqpoiewraflkfajpqwoeijrpqowejr;ladsjf;lasjfd;lakjfdspqowiejrpqowejf;alsdkjf;lakdsfntime";
          obj[2] = "disabled";
          data.add(obj);
      } */

      setDeployments(cliGuiCtx);
    }

    private void setDeployments(CliGuiContext cliGuiCtx) {
        ModelNode deploymentsQuery = null;
        String queryString = "/deployment=*/:read-resource";

        try {
            deploymentsQuery = cliGuiCtx.getExecutor().doCommand(queryString);
            if (deploymentsQuery.get("outcome").asString().equals("failed")) return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (ModelNode node : deploymentsQuery.get("result").asList()) {
            ModelNode deploymentNode = node.get("result"); // get the inner result
            Object[] deployment = new Object[3];
            data.add(deployment);

            String name = deploymentNode.get("name").asString();

            JRadioButton radio = new JRadioButton(name);
            deployment[0] = radio;
            deploymentsButtonGroup.add(radio);

            deployment[1] = deploymentNode.get("runtime-name").asString();

            ModelNode enabled = deploymentNode.get("enabled");
            if (enabled.isDefined()) deployment[2] = deploymentNode.get("enabled").asString();
        }

        if (data.size() > 0) {
            JRadioButton first = (JRadioButton)data.get(0)[0];
            first.setSelected(true);
        }
    }

    public int getColumnCount() {
        return 3;
    }

    public int getRowCount() {
        return data.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex)[columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return JRadioButton.class;
        return String.class;
    }

    public boolean hasDeployments() {
        return !data.isEmpty();
    }

    public String getSelectedDeployment() {
        if (!hasDeployments()) return null;

        for (Enumeration e=deploymentsButtonGroup.getElements(); e.hasMoreElements();) {
            JRadioButton radio = (JRadioButton)e.nextElement();
            if (radio.getModel() == deploymentsButtonGroup.getSelection()) {
                return radio.getText();
            }
        }

        throw new IllegalStateException("No deployment selected");
    }
}
