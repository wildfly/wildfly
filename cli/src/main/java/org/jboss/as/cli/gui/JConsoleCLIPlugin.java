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

import com.sun.tools.jconsole.JConsoleContext;
import com.sun.tools.jconsole.JConsolePlugin;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanServerConnection;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.remote.ExistingChannelModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.jboss.remotingjmx.RemotingMBeanServerConnection;
import org.jboss.threads.JBossThreadFactory;
import org.xnio.OptionMap;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JConsoleCLIPlugin extends JConsolePlugin {

    private static final int DEFAULT_MAX_THREADS = 6;

    // Global count of created pools
    private static final AtomicInteger executorCount = new AtomicInteger();

    CliGuiContext cliGuiCtx;
    private JPanel jconsolePanel;
    private boolean initComplete = false;
    private boolean isConnected = false;

    @Override
    public Map<String,JPanel> getTabs() {
        Map<String, JPanel> panelMap = new HashMap<String, JPanel>();

        final CommandContext cmdCtx;
        try {
            cmdCtx = CommandContextFactory.getInstance().newCommandContext();
            isConnected = connectCommandContext(cmdCtx);
            if (!isConnected) return panelMap;
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to JBoss AS.", e);
        }

        cliGuiCtx = GuiMain.startEmbedded(cmdCtx);
        JPanel cliGuiPanel = cliGuiCtx.getMainPanel();

        jconsolePanel = new JPanel(new BorderLayout());
        jconsolePanel.add(GuiMain.makeMenuBar(cliGuiCtx), BorderLayout.NORTH);
        jconsolePanel.add(cliGuiPanel, BorderLayout.CENTER);

        panelMap.put(getJBossServerName(), jconsolePanel);
        return panelMap;
    }

    private boolean connectCommandContext(CommandContext cmdCtx) throws Exception {
        JConsoleContext jcCtx = this.getContext();
        MBeanServerConnection mbeanServerConn = jcCtx.getMBeanServerConnection();

        if (mbeanServerConn instanceof RemotingMBeanServerConnection) {
            connectUsingRemoting(cmdCtx, (RemotingMBeanServerConnection)mbeanServerConn);
        } else {
            try {
                connectUsingDefaults(cmdCtx);
            } catch (Exception e) {
                String message = "Unable to connect to JBoss AS. \n";
                message += "Go to Connection -> New Connection and enter a Remote Process \n";
                message += "of the form service:jmx:remoting-jmx://{host_name}:{port}  where \n";
                message += "{host_name} and {port} are the address of the native management \n";
                message += "interface of the AS7 installation being monitored.";
                JOptionPane.showMessageDialog(cliGuiCtx.getMainWindow(), message);
                return false;
            }
        }

        return true;
    }

   private void connectUsingRemoting(CommandContext cmdCtx, RemotingMBeanServerConnection rmtMBeanSvrConn) throws IOException, CliInitializationException {
        Connection conn = rmtMBeanSvrConn.getConnection();
        Channel channel = conn.openChannel("management", OptionMap.EMPTY).get();
        ModelControllerClient modelCtlrClient = ExistingChannelModelControllerClient.createReceiving(channel, createExecutor());
        cmdCtx.bindClient(modelCtlrClient);
    }

    private ExecutorService createExecutor() {
        final ThreadGroup group = new ThreadGroup("management-client-thread");
        final ThreadFactory threadFactory = new JBossThreadFactory(group, Boolean.FALSE, null, "%G " + executorCount.incrementAndGet() + "-%t", null, null, AccessController.getContext());
        return new ThreadPoolExecutor(2, DEFAULT_MAX_THREADS, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    private void connectUsingDefaults(CommandContext cmdCtx) throws Exception {
        cmdCtx.connectController("localhost", 9999);
    }

    @Override
    public SwingWorker<?, ?> newSwingWorker() {
        if (!initComplete && isConnected) {
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
                frame.setTitle(getJBossServerName());
                return;
            }
        }
    }

    private String getJBossServerName() {
        String serverNamePrefix = "JBoss CLI / ";
        String serverNameCommand = "/:read-attribute(name=name,include-defaults=true)";
        if (!cliGuiCtx.isStandalone()) {
            serverNameCommand = "/host=*" + serverNameCommand;
        }

        try {
            ModelNode result = cliGuiCtx.getExecutor().doCommand(serverNameCommand);
            String outcome = result.get("outcome").asString();
            if (outcome.equals("success") && cliGuiCtx.isStandalone()) {
                return serverNamePrefix + result.get("result").asString();
            } else if (outcome.equals("success")) {
                return serverNamePrefix + result.get("result").asList().get(0).get("result").asString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return serverNamePrefix + "<unknown>";
    }

}
