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

        final WebArchive war = createWAR(SampleBean.class);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_UNIT_NAME);
        ear.addAsManifestResource(tccl.getResource(DEPLOYMENT_RESOURCES + "/application.xml"), "application.xml");
        ear.addAsModule(war);
        System.out.println(ear.toString(true));
        return ear;
    }

}
