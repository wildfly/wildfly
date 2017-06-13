package org.jboss.as.test.integration.deployment.classloading.jaxp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.as.test.integration.deployment.classloading.jaxp.FactoryServlet.Factory;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.Assert;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class JaxpTestUtils {
    private static Logger log = Logger.getLogger(JaxpTestUtils.class);
    static final File[] SAXON6_LIBS;
    static final File[] SAXON9_LIBS;
    static final File[] XALAN_LIBS;
    static final File[] XERCES_LIBS;
    private static final boolean IS_IBM_JVM = System.getProperty("java.vm.name").contains("IBM J9");

    static {
        File pom = new File("../../pom.xml").getAbsoluteFile();
        final PomEquippedResolveStage resolver = Maven.resolver().loadPomFromFile(pom, "jaxp.test.dependencies.profile");
        XERCES_LIBS = resolver.resolve("xerces:xercesImpl").withoutTransitivity().asFile();
        System.out.println("XERCES_LIBS = "+ Arrays.toString(XERCES_LIBS));
        XALAN_LIBS = new File[] {resolver.resolve("xalan:xalan").withoutTransitivity().asSingleFile(), resolver.resolve("xalan:serializer").withoutTransitivity().asSingleFile()};
        System.out.println("XALAN_LIBS = "+ Arrays.toString(XALAN_LIBS));
        SAXON6_LIBS = resolver.resolve("saxon:saxon").withoutTransitivity().asFile();
        System.out.println("SAXON6_LIBS = "+ Arrays.toString(SAXON6_LIBS));
        SAXON9_LIBS = resolver.resolve("net.sf.saxon:Saxon-HE").withoutTransitivity().asFile();
        System.out.println("SAXON9_LIBS = "+ Arrays.toString(SAXON9_LIBS));
    }

    static WebArchive baseApp(String name, Asset jbossDeploymentStructure) {
        JavaArchive sameLoaderJar = ShrinkWrap.create(JavaArchive.class) //
                .addClass(EmbeddedJarMethods.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, name) //
                .addAsManifestResource(jbossDeploymentStructure, "jboss-deployment-structure.xml") //
                .addClass(TransformerServlet.class) //
                .addClass(FactoryServlet.class) //
                .addClass(FileUtils.class) //
                .addPackage(Assert.class.getPackage())
                .addAsResource(EmbeddedJaxpImplTestCase.class.getResource("transform.xsl"), "transform.xsl") //
                .addAsResource(EmbeddedJaxpImplTestCase.class.getResource("input.xml"), "input.xml") //
                .addAsResource(EmbeddedJaxpImplTestCase.class.getResource("output.xml"), "output.xml") //
                .addAsResource(EmbeddedJaxpImplTestCase.class.getResource("schema.xsd"), "schema.xsd") //
                .addAsLibrary(sameLoaderJar);
        return war;
    }

    /**
     * An HTTP GET against {@code TestSuiteEnvironment.getHttpUrl() + "/"+ appName + "/" + servlet + queryString}.
     * @param appName the context to use when building the URI
     * @param servlet the servlet path
     * @param queryString the query string, may be {@code null}
     * @return the body of the response
     * @throws IOException
     * @throws TransformerException
     */
    static String get(String appName, String servlet, String queryString) throws IOException, TransformerException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final String requestURL = TestSuiteEnvironment.getHttpUrl().toString() + "/" + appName + "/" + servlet
                    + (queryString == null ? "" : queryString);
            log.debugf("getting %s", requestURL);
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                return "status "+ statusCode;
            }
            final HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            return EntityUtils.toString(entity);
        }
    }

    /**
     * Returns the body of the response got from {@link FactoryServlet}
     *
     * @param appName the test app to sent a request to
     * @param factory the factory to instantiate
     * @return the body of the response got from {@link FactoryServlet}
     * @throws IOException
     * @throws TransformerException
     */
    static String getFactory(String appName, Factory factory) throws IOException, TransformerException {
        return get(appName, "factory", "?factory="+ factory.toString());
    }

    /**
     * Returns the body of the response got from {@link FactoryServlet}
     *
     * @param appName the test app to sent a request to
     * @param factory the factory to instantiate
     * @param impl the name of the factory implementation class
     * @return the body of the response got from {@link FactoryServlet}
     * @throws IOException
     * @throws TransformerException
     */
    static String getFactory(String appName, Factory factory, String impl) throws IOException, TransformerException {
        return get(appName, "factory", "?factory="+ factory.toString()+"&impl="+ impl);
    }

    /**
     * Returns the body of the response got from {@link TransformerServlet}
     *
     * @param appName the test app to sent a request to
     * @return the body of the response got from {@link TransformerServlet}
     * @throws IOException
     * @throws TransformerException
     */
    static String getTransformer(String appName) throws IOException, TransformerException {
        return get(appName, "transformer", null);
    }

    /**
     * Returns the body of the response got from {@link TransformerServlet}
     *
     * @param appName the test app to sent a request to
     * @param factoryImpl a custom factory implementation to instantiate
     * @return the body of the response got from {@link TransformerServlet}
     * @throws IOException
     * @throws TransformerException
     */
    static String getTransformer(String appName, String factoryImpl) throws IOException, TransformerException {
        return get(appName, "transformer", "?impl="+ factoryImpl);
    }

    static boolean isIbmJvm() {
        return IS_IBM_JVM;
    }

}
