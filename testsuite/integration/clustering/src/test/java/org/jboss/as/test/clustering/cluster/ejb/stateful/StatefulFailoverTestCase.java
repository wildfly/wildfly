/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.CounterDecorator;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.IncrementorDDInterceptor;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.PersistenceIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.stateful.servlet.StatefulServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class StatefulFailoverTestCase extends AbstractStatefulFailoverTestCase {

    private static final String MODULE_NAME = StatefulFailoverTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(Incrementor.class.getPackage());
        war.addPackage(StatefulServlet.class.getPackage());
        war.addPackage(EJBDirectory.class.getPackage());
        war.setWebXML(StatefulServlet.class.getPackage(), "web.xml");
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\">" +
                "<interceptors><class>" + IncrementorDDInterceptor.class.getName() + "</class></interceptors>" +
                "<decorators><class>" + CounterDecorator.class.getName() + "</class></decorators>" +
                "</beans>"), "beans.xml");
        war.addAsResource(PersistenceIncrementorBean.class.getPackage(), "persistence.xml", "/META-INF/persistence.xml");
        return war;
    }

    public StatefulFailoverTestCase() {
        super(MODULE_NAME);
    }
}
