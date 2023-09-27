/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.cdi;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.transaction.TransactionMode;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.cluster.cdi.webapp.IncrementorBean;
import org.jboss.as.test.clustering.cluster.cdi.webapp.CDISerializationContextInitializer;
import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.web.AbstractWebFailoverTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Test failover with CDI session scoped bean using ProtoStream.
 *
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class ProtoStreamCDIFailoverTestCase extends AbstractWebFailoverTestCase {

    private static final String MODULE_NAME = CDIFailoverTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".ear";
    private static final String WEB_DEPLOYMENT_NAME = MODULE_NAME + ".war";

    public ProtoStreamCDIFailoverTestCase() {
        super(DEPLOYMENT_NAME + '.' + WEB_DEPLOYMENT_NAME, TransactionMode.TRANSACTIONAL);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_3)
    public static Archive<?> deployment3() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_DEPLOYMENT_NAME);
        war.addPackage(IncrementorBean.class.getPackage());
        war.addClasses(Incrementor.class, SimpleServlet.class, Mutable.class);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        war.setWebXML(CDIFailoverTestCase.class.getPackage(), "web.xml");
        war.addAsWebInfResource(CDIFailoverTestCase.class.getPackage(), "distributable-web.xml", "distributable-web.xml");
        war.addAsServiceProvider(SerializationContextInitializer.class.getName(), CDISerializationContextInitializer.class.getName() + "Impl");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT_NAME);
        ear.addAsModule(war);
        return ear;
    }
}
