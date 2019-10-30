/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.integration.batch.common;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.JobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Assert;

@Named("L2")
public class JobListener2 implements JobListener {
    @Inject @BatchProperty(name="job-prop")
    private String jobProp = "L2";  //unmatched property

    @Inject @BatchProperty(name = "listener-prop")
    private String listenerProp;  //nothing is injected

    @Inject @BatchProperty(name = "reference-job-prop")
    private String referencedProp;  //nothing is injected

    @Inject
    private JobContext jobContext;

    @Override
    public void beforeJob() throws Exception {
        //Assert.assertEquals("L2", jobProp);  should be null or "L2"?
        Assert.assertEquals(null, listenerProp);
        Assert.assertEquals(null, referencedProp);
        Assert.assertEquals(2, jobContext.getProperties().size());
        Assert.assertEquals("job-prop", jobContext.getProperties().get("job-prop"));
    }

    @Override
    public void afterJob() throws Exception {
    }
}
