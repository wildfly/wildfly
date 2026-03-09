/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jsf;

import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.clustering.cluster.jsf.webapp.Game;
import org.jboss.as.test.clustering.cluster.jsf.webapp.JSFSerializationContextInitializer;
import org.jboss.as.test.clustering.cluster.web.DistributableTestCase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
public class ProtoStreamJSFFailoverTestCase extends AbstractJSFFailoverTestCase {

    private static final String MODULE_NAME = ProtoStreamJSFFailoverTestCase.class.getSimpleName();

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

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(Game.class.getPackage());
        war.setWebXML(DistributableTestCase.class.getPackage(), "web.xml");
        war.addAsWebResource(AbstractJSFFailoverTestCase.class.getPackage(), "home.xhtml", "home.xhtml");
        war.addAsWebInfResource(AbstractJSFFailoverTestCase.class.getPackage(), "faces-config.xml", "faces-config.xml");
        war.addAsWebInfResource(AbstractJSFFailoverTestCase.class.getPackage(), "distributable-web.xml", "distributable-web.xml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addAsServiceProvider(SerializationContextInitializer.class.getName(), JSFSerializationContextInitializer.class.getName() + "Impl");
        return war;
    }

}
