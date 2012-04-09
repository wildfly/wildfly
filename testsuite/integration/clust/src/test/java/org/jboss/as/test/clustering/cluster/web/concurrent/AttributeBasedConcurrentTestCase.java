/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.web.concurrent;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;
import org.jboss.as.test.clustering.cluster.web.ClusteredWebSimpleTestCase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Radoslav Husar
 * @version April 2012
 */
public class AttributeBasedConcurrentTestCase extends ConcurrentAbstractCase {

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "concurrent-session-replication.war");
        // Take web.xml from the managed test.
        war.setWebXML(ClusteredWebSimpleTestCase.class.getPackage(), "web.xml");
        war.addAsWebInfResource(ClusteredWebSimpleTestCase.class.getPackage(), "jboss-web_granular.xml", "jboss-web.xml");
        // Bundle the JSPs
        war.addAsWebResource(ConcurrentAbstractCase.class.getPackage(), "getattribute.jsp", "getattribute.jsp");
        war.addAsWebResource(ConcurrentAbstractCase.class.getPackage(), "testsessionreplication.jsp", "testsessionreplication.jsp");
        System.out.println(war.toString(true));
        return war;
    }
}
