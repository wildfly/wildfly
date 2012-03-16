package org.jboss.as.test.integration.jaxrs.cfg;

import java.util.concurrent.TimeUnit;
import java.net.URL;

import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.HelloWorldResource;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public abstract class ResteasyParamsParent {

	private final static String DEP_NAME = "default";

	public static Archive<?> getDeploy(final String depName,
			final Class<?> clazz, final String paramName,
			final String paramValue) {
		return getDeploy(depName, clazz, paramName, paramValue, "", "");
	}

	public static Archive<?> getDeploy(final Class<?> clazz,
			final String paramName, final String paramValue) {
		return getDeploy(DEP_NAME, clazz, paramName, paramValue, "", "");
	}

	public static Archive<?> getDeploy(final Class<?> clazz,
			final String paramName, final String paramValue, final String webXmlBefore, final String webXmlAfter) {
		return getDeploy(DEP_NAME, clazz, paramName, paramValue, webXmlBefore, webXmlAfter);
	}

	public static Archive<?> getDeploy(final String depName,
			final Class<?> clazz, final String paramName,
			final String paramValue, final String webXmlBefore, final String webXmlAfter) {
		WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsnoap_"
				+ (depName == null ? DEP_NAME : depName.length() > 0 ? depName
						: DEP_NAME) + ".war");
		war.addPackage(HttpRequest.class.getPackage());
		war.addClasses(clazz, ResteasyParamsParent.class, HelloWorldResource.class);
		war.addAsWebInfResource(
				WebXml.get(webXmlBefore 
						+ "<servlet-mapping>\n"
						+ "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
						+ "        <url-pattern>/myjaxrs/*</url-pattern>\n"
						+ "</servlet-mapping>\n" 
						+ "\n" 
						+ "<context-param>\n"
						+ "        <param-name>" + paramName + "</param-name>\n" 
						+ "        <param-value>" + paramValue + "</param-value>\n"
						+ "</context-param>\n"
						+ webXmlAfter
						+ "\n"), "web.xml");
		return war;
	}

	public static String performCall(final String urlPattern, URL url)
			throws Exception {
		return HttpRequest.get(url + urlPattern, 5, TimeUnit.SECONDS);
	}

}
