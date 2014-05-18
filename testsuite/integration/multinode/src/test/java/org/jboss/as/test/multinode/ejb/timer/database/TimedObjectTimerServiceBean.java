/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.multinode.ejb.timer.database;

import org.jboss.as.test.shared.FileUtils;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TimedObject;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.Date;

/**
 * @author Stuart Douglas
 */
@Stateless
public class TimedObjectTimerServiceBean implements TimedObject, RemoteTimedBean {

    @Resource
    private TimerService timerService;

    private volatile boolean run = false;

    @EJB(lookup = "ejb:/testTimerServiceSimple/CollectionSingleton!org.jboss.as.test.multinode.ejb.timer.database.Collector")
    private Collector collector;

    private static final String NODE;

    static {
        NODE = FileUtils.readFile(TimedObjectTimerServiceBean.class.getClassLoader().getResource("node.txt"));
    }

    @Override
    public void scheduleTimer(long date, String info) {
        timerService.createSingleActionTimer(new Date(date), new TimerConfig(info, true));
    }

    @Override
    public boolean hasTimerRun() {
        return run;
    }

    @Override
    public void ejbTimeout(final Timer timer) {
        run = true;
        collector.timerRun(NODE, (String) timer.getInfo());

    }
}
