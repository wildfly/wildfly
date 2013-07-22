/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JTabbedPane;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.as.cli.gui.ManagementModel;
import org.jboss.as.cli.gui.ManagementModelNode;
import org.jboss.as.cli.gui.component.ButtonTabComponent;

/**
 * Action that creates a new tab with the given node as its root.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class ExploreNodeAction extends AbstractAction {

    private ManagementModelNode node;
    private CliGuiContext cliGuiCtx;

    public ExploreNodeAction(CliGuiContext cliGuiCtx) {
        super(calcActionName(cliGuiCtx));
        this.node = findSelectedNode(cliGuiCtx);
        this.cliGuiCtx = cliGuiCtx;
    }

    public void actionPerformed(ActionEvent ae) {
        JTabbedPane tabs = cliGuiCtx.getTabs();
        int newTabIndex = tabs.getSelectedIndex() + 1;
        ManagementModelNode newRoot = node.clone();
        tabs.insertTab(calcTabName(this.node), null, new ManagementModel(newRoot, cliGuiCtx), newRoot.addressPath(), newTabIndex);
        tabs.setTabComponentAt(newTabIndex, new ButtonTabComponent(tabs));
        tabs.setSelectedIndex(newTabIndex);
    }

    public ManagementModelNode getSelectedNode() {
        return this.node;
    }

    private static String calcActionName(CliGuiContext cliGuiCtx) {
        ManagementModelNode node = findSelectedNode(cliGuiCtx);
        if (node == null) return "Explore selected node";
        return "Explore " + calcTabName(node);
    }

    private static ManagementModelNode findSelectedNode(CliGuiContext cliGuiCtx) {
        Component selectedComponent = cliGuiCtx.getTabs().getSelectedComponent();
        if (selectedComponent == null) return null;
        if (!(selectedComponent instanceof ManagementModel)) return null;

        return ((ManagementModel)selectedComponent).getSelectedNode();
    }

    private static String calcTabName(ManagementModelNode node) {
        ManagementModelNode.UserObject usrObj = (ManagementModelNode.UserObject) node.getUserObject();
        if (usrObj.isGeneric()) {
            return node.toString();
        }
        if (usrObj.isLeaf()) {
            return usrObj.getName();
        }
        return node.toString();
    }
}
