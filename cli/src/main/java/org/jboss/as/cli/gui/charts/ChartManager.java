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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ChartManager {
    private List<JBossChart> chartList = new ArrayList<JBossChart>();

    private CliGuiContext cliGuiCtx;

    public ChartManager(CliGuiContext cliGuiCtx) {
        this.cliGuiCtx = cliGuiCtx;
    }

    public void addChart(JBossChart jbossChart) {
        JTabbedPane tabs = cliGuiCtx.getTabs();
        int tabCount = tabs.getComponentCount();

        tabs.add(jbossChart.getName(), makeTabPanel(jbossChart));
        tabs.setSelectedIndex(tabCount);
        this.chartList.add(jbossChart);
    }

    private JPanel makeTabPanel(JBossChart jbossChart) {
        final JPanel tabPanel = new JPanel();
        tabPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        JButton closeButton = makeCloseButton(jbossChart, tabPanel);
        tabPanel.add(closeButton, gbc);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        tabPanel.add(jbossChart.getChartPanel(), gbc);

        jbossChart.getChartPanel().getChart().addChangeListener(new ChartChangeListener() {
            public void chartChanged(ChartChangeEvent cce) {
                tabPanel.repaint();
            }
        });

        return tabPanel;
    }

    private JButton makeCloseButton(JBossChart jbossChart, JPanel tabPanel) {
        JButton close = new JButton("x");
        close.addActionListener(new CloseButtonListener(jbossChart, tabPanel));
        close.setToolTipText("Close Graph");

        return close;
    }

    private class CloseButtonListener implements ActionListener {
        private JBossChart jbossChart;
        private JPanel tabPanel;

        public CloseButtonListener(JBossChart jbossChart, JPanel tabPanel) {
            this.jbossChart = jbossChart;
            this.tabPanel = tabPanel;
        }

        public void actionPerformed(ActionEvent e) {
            JTabbedPane tabs = cliGuiCtx.getTabs();
            tabs.remove(tabPanel);
            chartList.remove(jbossChart);
        }
    }

    public void updateCharts() throws Exception {
        for (JBossChart chart : chartList) {
            chart.update();
        }
    }
}
