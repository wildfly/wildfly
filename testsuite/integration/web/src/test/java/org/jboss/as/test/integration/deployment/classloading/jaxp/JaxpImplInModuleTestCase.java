package org.jboss.as.test.integration.deployment.classloading.jaxp;

import java.io.IOException;
import java.util.Random;

import javax.xml.transform.TransformerException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.deployment.classloading.jaxp.FactoryServlet.Factory;
import org.jboss.as.test.integration.deployment.classloading.jaxp.JBossDeploymentStructure.Services;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Asserts that a web application that depends on a module that contains an implementation of a JAXP service works
 * properly.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(JaxpImplInModuleTestCase.JaxpImplInModuleSetupTask.class)
@RunAsClient
public class JaxpImplInModuleTestCase {

    public static class JaxpImplInModuleSetupTask extends JaxpClassLoadingSetupTask {

        public JaxpImplInModuleSetupTask() {
            super( //
                    new TestModule[] { //
                            TestModule.builder(DEPENDENCY_MODULE_NAME) //
                                    .javaArchive( //
                                            ShrinkWrap
                                                    .create(JavaArchive.class,
                                                            CustomModuleMethods.class.getSimpleName() + ".jar") //
                                                    .addClass(CustomModuleMethods.class) //
                                    ) //
                                    .build(), //
                            TestModule.builder(SAXON9_MODULE_NAME) //
                                    .dependency("javax.api") //
                                    .javaArchives(JaxpTestUtils.SAXON9_LIBS) //
                                    .build() //
                    }, //
                    new WebArchive[] { WF_XALAN_APP, SAXON9_APP, WF_XERCES_APP} //
            );
        }

    }

    private static final String DEPENDENCY_MODULE_NAME;

    private static final WebArchive SAXON9_APP;
    private static final String SAXON9_MODULE_NAME;

    private static final WebArchive WF_XALAN_APP;
    private static final String WF_XALAN_MODULE_NAME = "org.apache.xalan";
    private static final WebArchive WF_XERCES_APP;
    private static final String WF_XERCES_MODULE_NAME = "org.apache.xerces";

    static {
        /* Let's add some random suffix to the resource names here so that we can run without cleanup */
        final Random rnd = new Random();
        final String prefix = JaxpImplInModuleTestCase.class.getName();

        DEPENDENCY_MODULE_NAME = CustomModuleMethods.class.getName() + "_" + rnd.nextInt(Integer.MAX_VALUE);

        SAXON9_MODULE_NAME = prefix + ".saxon9_" + rnd.nextInt(Integer.MAX_VALUE);

        final String appNamePrefix = JaxpImplInModuleTestCase.class.getSimpleName();
        WF_XALAN_APP = JaxpTestUtils.baseApp(appNamePrefix + "-wf-xalan-" + rnd.nextInt(Integer.MAX_VALUE),
                new JBossDeploymentStructure() //
                        .dependency(DEPENDENCY_MODULE_NAME) //
                        .dependency(WF_XALAN_MODULE_NAME, Services.import_) //
                        .asAsset() //
        );

        WF_XERCES_APP = JaxpTestUtils.baseApp(appNamePrefix + "-wf-xerces-" + rnd.nextInt(Integer.MAX_VALUE),
                new JBossDeploymentStructure() //
                        .dependency(DEPENDENCY_MODULE_NAME) //
                        .dependency(WF_XERCES_MODULE_NAME, Services.import_) //
                        .asAsset() //
        );

        SAXON9_APP = JaxpTestUtils.baseApp(appNamePrefix + "-saxon9-" + rnd.nextInt(Integer.MAX_VALUE),
                new JBossDeploymentStructure() //
                        .dependency(DEPENDENCY_MODULE_NAME) //
                        .dependency("javax.api") //
                        .dependency(SAXON9_MODULE_NAME, Services.import_) //
                        .asAsset() //
        ) //
                .addAsResource(EmbeddedJaxpImplTestCase.class.getResource("transform-saxon9.xsl"), "transform.xsl") //
                .addAsResource(EmbeddedJaxpImplTestCase.class.getResource("output-saxon9.xml"), "output.xml") //
        ;
    }

    @Deployment // needed because otherwise Arquillian does not call the JaxpImplInModuleSetupTask
    public static Archive<?> dummy() {
        return ShrinkWrap.create(WebArchive.class);
    }


    @Test
    @RunAsClient
    @InSequence(20)
    public void saxon9() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(SAXON9_APP.getName());
        Assert.assertEquals("net.sf.saxon.jaxp.TransformerImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(21)
    public void saxon9WithExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(SAXON9_APP.getName(), "net.sf.saxon.TransformerFactoryImpl");
        Assert.assertEquals("net.sf.saxon.jaxp.TransformerImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(0)
    public void wfXalan() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(WF_XALAN_APP.getName());
        final String expected = JaxpTestUtils.isIbmJvm() ? "com.ibm.xtq.xslt.jaxp.TransformerImpl"
                : "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl";
        Assert.assertEquals(expected, actual);
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void wfXalanExplicitFactory() throws IOException, TransformerException {
        final String impl = JaxpTestUtils.isIbmJvm() ? "com.ibm.xtq.xslt.jaxp.compiler.TransformerFactoryImpl"
                : "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
        final String actual = JaxpTestUtils.getTransformer(WF_XALAN_APP.getName(), impl);
        final String expected = JaxpTestUtils.isIbmJvm() ? "com.ibm.xtq.xslt.jaxp.TransformerImpl"
                : "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl";
        Assert.assertEquals(expected, actual);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void wfXalanXPath() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XALAN_APP.getName(), Factory.XPath);
        /* IBM JVM delivers org.apache.xpath.jaxp.XPathImpl in lib/xml.jar */
        final String expected = JaxpTestUtils.isIbmJvm() ? "org.apache.xpath.jaxp.XPathImpl"
                : "com.sun.org.apache.xpath.internal.jaxp.XPathImpl";
        Assert.assertEquals(expected, actual);
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void wfXalanXPathExplicitFactory() throws IOException, TransformerException {
        final String impl = JaxpTestUtils.isIbmJvm() ? "com.ibm.xtq.xpath.jaxp.XPathFactoryImpl"
                : "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl";
        final String actual = JaxpTestUtils.getFactory(WF_XALAN_APP.getName(), Factory.XPath, impl);
        final String expected = JaxpTestUtils.isIbmJvm() ? "com.ibm.xtq.xpath.jaxp.v1.XPath10Impl"
                : "com.sun.org.apache.xpath.internal.jaxp.XPathImpl";
        Assert.assertEquals(expected, actual);
    }

    @Test
    @RunAsClient
    @InSequence(31)
    public void wfXercesDatatype() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.Datatype);
        Assert.assertEquals("org.apache.xerces.jaxp.datatype.XMLGregorianCalendarImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(32)
    public void wfXercesDatatypeExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.Datatype, "org.apache.xerces.jaxp.datatype.DatatypeFactoryImpl");
        Assert.assertEquals("org.apache.xerces.jaxp.datatype.XMLGregorianCalendarImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(33)
    public void wfXercesDocumentBuilder() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.DocumentBuilder);
        Assert.assertEquals("org.apache.xerces.jaxp.DocumentBuilderImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(34)
    public void wfXercesDocumentBuilderExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.DocumentBuilder, "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        Assert.assertEquals("org.apache.xerces.jaxp.DocumentBuilderImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(35)
    public void wfXercesSAXParser() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.SAXParser);
        Assert.assertEquals("org.apache.xerces.jaxp.SAXParserImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(36)
    public void wfXercesSAXParserExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.SAXParser, "org.apache.xerces.jaxp.SAXParserFactoryImpl");
        Assert.assertEquals("org.apache.xerces.jaxp.SAXParserImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(37)
    public void wfXercesSchema() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.Schema);
        Assert.assertEquals("org.apache.xerces.jaxp.validation.SimpleXMLSchema", actual);
    }

    @Test
    @RunAsClient
    @InSequence(38)
    public void wfXercesSchemaExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.Schema, "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        Assert.assertEquals("org.apache.xerces.jaxp.validation.SimpleXMLSchema", actual);
    }

    @Test
    @RunAsClient
    @InSequence(39)
    public void wfXercesXMLEvent() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.XMLEvent);
        Assert.assertEquals("org.codehaus.stax2.ri.evt.StartDocumentEventImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(40)
    public void wfXercesXMLReader() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(WF_XERCES_APP.getName(), Factory.XMLReader);
        Assert.assertEquals("org.apache.xerces.parsers.AbstractSAXParser$AttributesProxy", actual);
    }

}
