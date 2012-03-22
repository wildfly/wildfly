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
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import org.jboss.as.cli.gui.GuiMain;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ServerGroupChooser extends JPanel {

    private List<JCheckBox> serverGroups = new ArrayList<JCheckBox>();
    private JPanel serverGroupsPanel = new JPanel(new FlowLayout());;

    public ServerGroupChooser() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Server Groups"));
        setServerGroups();
        add(serverGroupsPanel, BorderLayout.CENTER);
        if (!isStandalone()) serverGroups.get(0).setSelected(true);  // at least one must be selected
    }

    private void setServerGroups() {
        Set<String> serverGroupNames = new TreeSet<String>();
        try {
            ModelNode serverGroupQuery = GuiMain.getExecutor().doCommand("/:read-children-names(child-type=server-group)");
            if (serverGroupQuery.get("outcome").asString().equals("failed")) return;

            for (ModelNode node : serverGroupQuery.get("result").asList()) {
                serverGroupNames.add(node.asString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // make sorted server gorup names into checkboxes
        for (String name : serverGroupNames) {
            JCheckBox serverGroupCheckBox = new JCheckBox(name);
            serverGroups.add(serverGroupCheckBox);
            serverGroupsPanel.add(serverGroupCheckBox);
        }

    }

    // TODO: FixMe.  This is not safe because the caller can change the list
    // and state of the checkboxes.
    public List<JCheckBox> getServerGroups() {
        return this.serverGroups;
    }

    public final boolean isStandalone() {
        return serverGroups.isEmpty();
    }

    public boolean allServerGroupsChecked() {
        for (JCheckBox serverGroup : serverGroups) {
            if (!serverGroup.isSelected()) return false;
        }

        return true;
    }
}
