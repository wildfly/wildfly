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

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.as.cli.gui.ManagementModelNode;
import org.jboss.as.cli.gui.ManagementModelNode.UserObject;
import org.jboss.dmr.ModelNode;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * This class wraps a JFreeChart.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JBossChart {

    private static Image backgroundImage;
    static {
        URL iconURL = JBossChart.class.getResource("/icon/jbossas7_ligature_r5v1.jpg");
        backgroundImage = Toolkit.getDefaultToolkit().getImage(iconURL);
    }

    private CliGuiContext cliGuiCtx;
    private String name;
    private String title;
    private String description;
    private String readAttrCmd;
    private String attrName;

    private TimeSeriesCollection dataset;
    private TimeSeries series;

    private ChartPanel chartPanel;

    public JBossChart(CliGuiContext cliGuiCtx, String name, String title, String description, ManagementModelNode node) {
        this.cliGuiCtx = cliGuiCtx;
        this.name = name;
        this.title = title;
        this.description = description;
        UserObject usrObj = (UserObject)node.getUserObject();
        this.attrName = usrObj.getName();
        this.readAttrCmd = node.addressPath() + ":read-attribute(name=" + attrName + ",include-defaults=true)";
        this.chartPanel = makeChart();
    }

    public String getName() {
        return this.name;
    }

    public String getTitle() {
        return this.title;
    }

    public ChartPanel getChartPanel() {
        return this.chartPanel;
    }

    private ChartPanel makeChart() {
        series = new TimeSeries(description);
        dataset = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title,
                                                              "Time of Day",
                                                              attrName,
                                                              dataset,
                                                              true,
                                                              true,
                                                              false);
        chart.getPlot().setBackgroundImage(backgroundImage);
        return new ChartPanel(chart);
    }

    /**
     * Get the latest data and update the chart.
     *
     * @throws Exception
     */
    public void update() throws Exception {
        ModelNode result = cliGuiCtx.getExecutor().doCommand(this.readAttrCmd);
        double newValue = result.get("result").asDouble();
        series.add(new FixedMillisecond(System.currentTimeMillis()), newValue);
    }
}
