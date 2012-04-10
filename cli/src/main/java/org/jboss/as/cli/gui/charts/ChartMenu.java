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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.as.cli.gui.ManagementModelNode;

/**
 * JPopupMenu that provides graphing for real time attributes.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ChartMenu extends JPopupMenu {
    private CliGuiContext cliGuiCtx;
    private JTree invoker;

    public ChartMenu(CliGuiContext cliGuiCtx, JTree invoker) {
        this.cliGuiCtx = cliGuiCtx;
        this.invoker = invoker;
        setLightWeightPopupEnabled(true);
        setOpaque(true);
    }

    /**
     * Show the OperationMenu based on the selected node.
     * @param node The selected node.
     * @param x The x position of the selection.
     * @param y The y position of the selection.
     */
    public void show(ManagementModelNode node, int x, int y) {
        removeAll();
        add(new OperationAction(node, "Real Time Graph", "Plot this attribute in a real time 2D graph."));
        super.show(invoker, x, y);
    }

    /**
     * Action for a menu selection.
     */
    private class OperationAction extends AbstractAction {

        private ManagementModelNode node;

        public OperationAction(ManagementModelNode node, String opName, String helpText) {
            super(opName);
            this.node = node;
            putValue(Action.SHORT_DESCRIPTION, helpText);
        }

        public void actionPerformed(ActionEvent ae) {
            CreateAChartDialog dialog = new CreateAChartDialog(cliGuiCtx, node);
            dialog.setLocationRelativeTo(cliGuiCtx.getMainWindow());
            dialog.setVisible(true);
        }
    }
}
