package org.jboss.as.test.integration.naming.remote.multiple;

import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

/**
 * Regression test for AS7-5718
 * @author jlivings@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleClientRemoteJndiTestCase {
	@ArquillianResource(RunRmiServlet.class)
	@OperateOnDeployment("one")
	private URL urlOne;

	@ArquillianResource(RunRmiServlet.class)
	@OperateOnDeployment("two")
	private URL urlTwo;

	private static final Package thisPackage = MultipleClientRemoteJndiTestCase.class.getPackage();

	@Deployment(name="one")
	public static WebArchive deploymentOne() {
		return ShrinkWrap.create(WebArchive.class, "one.war")
				.addClasses(RunRmiServlet.class, MyObject.class)
				.setWebXML(thisPackage, "web.xml")
				.addAsManifestResource(thisPackage, "war-jboss-deployment-structure.xml", "jboss-deployment-structure.xml");
	}
	@Deployment(name="two")
	public static WebArchive deploymentTwo() {
		return ShrinkWrap.create(WebArchive.class, "two.war")
				.addClasses(RunRmiServlet.class, MyObject.class)
				.setWebXML(thisPackage, "web.xml")
				.addAsManifestResource(thisPackage, "war-jboss-deployment-structure.xml", "jboss-deployment-structure.xml");
	}
	@Deployment(name="binder")
	public static WebArchive deploymentThree() {
		return ShrinkWrap.create(WebArchive.class, "binder.war")
				.addClasses(BindRmiServlet.class, MyObject.class)
				.setWebXML(MultipleClientRemoteJndiTestCase.class.getPackage(), "web.xml");
	}

	@Test
	public void testLifeCycle() throws Exception {
		String result1 = HttpRequest.get(urlOne.toExternalForm() + "RunRmiServlet", 1000, SECONDS);
		assertEquals("Test", result1);
		String result2 = HttpRequest.get(urlTwo.toExternalForm() + "RunRmiServlet", 1000, SECONDS);
		assertEquals("Test", result2);
	}
}
