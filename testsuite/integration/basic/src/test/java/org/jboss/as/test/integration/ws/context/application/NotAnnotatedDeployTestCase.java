package org.jboss.as.test.integration.ws.context.application;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class NotAnnotatedDeployTestCase extends ContextRootTestBase {

    @Deployment
    public static EnterpriseArchive createDeployment() {
        final WebArchive war = createWAR(SampleBean.class, "ws-notannotated-XXX.war");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ws-notannotated-XXX.ear");
        ear.addAsManifestResource(NotAnnotatedDeployTestCase.class.getPackage(), "application-notannotated.xml", "application.xml");
        ear.addAsModule(war);
        return ear;
    }

}
