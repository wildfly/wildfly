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
package org.jboss.as.cli.gui;

import com.sun.tools.jconsole.JConsolePlugin;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JConsoleCLIPlugin extends JConsolePlugin {

    private JPanel jconsolePanel;
    private boolean initComplete = false;

    @Override
    public Map<String,JPanel> getTabs() {
        final CommandContext cmdCtx;

        // hard code connection for now
        try {
            cmdCtx = CommandContextFactory.getInstance().newCommandContext();
            cmdCtx.connectController("localhost", 9999);
        } catch (CliInitializationException e) {
            throw new RuntimeException(e);
        } catch (CommandLineException e) {
            throw new RuntimeException(e);
        }

        JPanel cliGuiPanel = GuiMain.startEmbedded(cmdCtx);

        jconsolePanel = new JPanel(new BorderLayout());
        jconsolePanel.add(GuiMain.makeMenuBar(), BorderLayout.NORTH);
        jconsolePanel.add(cliGuiPanel, BorderLayout.CENTER);

        Map<String, JPanel> panelMap = new HashMap<String, JPanel>();
        panelMap.put(getJBossServerName(), jconsolePanel);
        return panelMap;
    }

    @Override
    public SwingWorker<?, ?> newSwingWorker() {
        if (!initComplete) {
            initComplete = true;
            configureMyJInternalFrame();
        }

        return null;
    }

    private void configureMyJInternalFrame() {
        ImageIcon icon = new ImageIcon(GuiMain.getJBossIcon());
        Component component = jconsolePanel;

        while (component != null) {
            component = component.getParent();
            if (component instanceof JInternalFrame) {
                JInternalFrame frame = (JInternalFrame)component;
                frame.setFrameIcon(icon);
                frame.setLayout(frame.getLayout());
                frame.setTitle(getJBossServerName());
                return;
            }
        }
    }

    private String getJBossServerName() {
        try {
            ModelNode result = GuiMain.getExecutor().doCommand("/:read-attribute(name=name,include-defaults=true)");
            String outcome = result.get("outcome").asString();
            if (outcome.equals("success")) {
                return "JBossAS / " + result.get("result").asString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "<unknown>";
    }

}
