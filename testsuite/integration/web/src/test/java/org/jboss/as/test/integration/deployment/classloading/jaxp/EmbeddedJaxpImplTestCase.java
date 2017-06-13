/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This tests whether implementations of various JAXP services work well when embedded in a web application.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(EmbeddedJaxpImplTestCase.EmbeddedJaxpImplSetupTask.class)
public class EmbeddedJaxpImplTestCase {

    static class EmbeddedJaxpImplSetupTask extends JaxpClassLoadingSetupTask {

        public EmbeddedJaxpImplSetupTask() {
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
                    }, //
                    new WebArchive[] { XALAN_APP, DEFAULT_APP, SAXON6_APP, SAXON9_APP, XERCES_APP } //
            );
        }
    }

    private static final String DEPENDENCY_MODULE_NAME;

    private static final WebArchive DEFAULT_APP;
    private static final WebArchive SAXON6_APP;
    private static final WebArchive SAXON9_APP;
    private static final WebArchive XALAN_APP;
    private static final WebArchive XERCES_APP;

    static {
        /* Let's add some random suffix to the resource names here so that we can run without cleanup */
        Random rnd = new Random();

        DEPENDENCY_MODULE_NAME = CustomModuleMethods.class.getName() + "_" + rnd.nextInt(Integer.MAX_VALUE);

        final String appNamePrefix = EmbeddedJaxpImplTestCase.class.getSimpleName();

        XALAN_APP = defaultApp(appNamePrefix + "-xalan-" + rnd.nextInt(Integer.MAX_VALUE)).addAsLibraries(JaxpTestUtils.XALAN_LIBS);
        XERCES_APP = defaultApp(appNamePrefix + "-xerces-" + rnd.nextInt(Integer.MAX_VALUE)).addAsLibraries(JaxpTestUtils.XERCES_LIBS);

        DEFAULT_APP = defaultApp(appNamePrefix + "-default-" + rnd.nextInt(Integer.MAX_VALUE));

        SAXON6_APP = defaultApp(appNamePrefix + "-saxon6-" + rnd.nextInt(Integer.MAX_VALUE))
                .addAsLibraries(JaxpTestUtils.SAXON6_LIBS);

        SAXON9_APP = defaultApp(appNamePrefix + "-saxon9-" + rnd.nextInt(Integer.MAX_VALUE)) //
                .addAsLibraries(JaxpTestUtils.SAXON9_LIBS) //
                .addAsResource(EmbeddedJaxpImplTestCase.class.getResource("transform-saxon9.xsl"), "transform.xsl") //
                .addAsResource(EmbeddedJaxpImplTestCase.class.getResource("output-saxon9.xml"), "output.xml") //
        ;

    }

    private static WebArchive defaultApp(String name) {
        return JaxpTestUtils.baseApp(name,
                Utils.getJBossDeploymentStructure(DEPENDENCY_MODULE_NAME));
    }

    @Deployment // needed because otherwise Arquillian does not call the EmbeddedJaxpImplSetupTask
    public static Archive<?> dummy() {
        return ShrinkWrap.create(WebArchive.class);
    }

    @Test
    @RunAsClient
    @InSequence(10)
    public void defaultTransformer() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(DEFAULT_APP.getName());
        Assert.assertEquals("org.apache.xalan.xsltc.trax.TransformerImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(11)
    public void defaultTransformerExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(DEFAULT_APP.getName(), "org.apache.xalan.xsltc.trax.TransformerImpl");
        Assert.assertEquals("status 500", actual);
    }

    @Test
    @RunAsClient
    @InSequence(12)
    public void defaultDatatype() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.Datatype);
        Assert.assertEquals("org.apache.xerces.jaxp.datatype.XMLGregorianCalendarImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(13)
    public void defaultDatatypeExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.Datatype, "org.apache.xerces.jaxp.datatype.DatatypeFactoryImpl");
        final String msg = "Provider org.apache.xerces.jaxp.datatype.DatatypeFactoryImpl not found";
        Assert.assertTrue("Expected "+ msg + "; actual "+ actual, actual.contains(msg));
    }

    @Test
    @RunAsClient
    @InSequence(14)
    public void defaultDocumentBuilder() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.DocumentBuilder);
        Assert.assertEquals("org.apache.xerces.jaxp.DocumentBuilderImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(15)
    public void defaultDocumentBuilderExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.DocumentBuilder, "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        Assert.assertEquals("status 500", actual);
    }

    @Test
    @RunAsClient
    @InSequence(16)
    public void defaultSAXParser() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.SAXParser);
        Assert.assertEquals("org.apache.xerces.jaxp.SAXParserImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(17)
    public void defaultSAXParserExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.SAXParser, "org.apache.xerces.jaxp.SAXParserFactoryImpl");
        Assert.assertEquals("status 500", actual);
    }

    @Test
    @RunAsClient
    @InSequence(18)
    public void defaultSAXTransformer() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.SAXTransformer);
        Assert.assertEquals("org.apache.xalan.xsltc.trax.TransformerImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(19)
    public void defaultSAXTransformerExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.SAXTransformer, "org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        Assert.assertEquals("status 500", actual);
    }

    @Test
    @RunAsClient
    @InSequence(20)
    public void defaultSchema() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.Schema);
        Assert.assertEquals("org.apache.xerces.jaxp.validation.SimpleXMLSchema", actual);
    }

    @Test
    @RunAsClient
    @InSequence(21)
    public void defaultSchemaExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.Schema, "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        Assert.assertEquals("org.apache.xerces.jaxp.validation.SimpleXMLSchema", actual);
    }

    @Test
    @RunAsClient
    @InSequence(22)
    public void defaultXMLEvent() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.XMLEvent);
        Assert.assertEquals("org.codehaus.stax2.ri.evt.StartDocumentEventImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(23)
    public void defaultXMLInput() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.XMLInput);
        Assert.assertEquals("com.ctc.wstx.sr.ValidatingStreamReader", actual);
    }

    @Test
    @RunAsClient
    @InSequence(24)
    public void defaultXMLOutput() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.XMLOutput);
        Assert.assertEquals("com.ctc.wstx.sw.SimpleNsStreamWriter", actual);
    }

    @Test
    @RunAsClient
    @InSequence(25)
    public void defaultXMLReader() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.XMLReader);
        Assert.assertEquals("org.apache.xerces.parsers.AbstractSAXParser$AttributesProxy", actual);
    }

    @Test
    @RunAsClient
    @InSequence(26)
    public void defaultXPath() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.XPath);
        Assert.assertEquals("org.apache.xpath.jaxp.XPathImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(27)
    public void defaultXPathExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(DEFAULT_APP.getName(), Factory.XPath, "org.apache.xpath.jaxp.XPathFactoryImpl");
        final String msg = "No XPathFactory implementation found";
        final String msgIbm = "Provider org.apache.xpath.jaxp.XPathFactoryImpl not found";
        Assert.assertTrue("Expected "+ msg + " or "+ msgIbm +"; actual "+ actual, actual.contains(msg) || actual.contains(msgIbm));
    }

    @Test
    @RunAsClient
    @InSequence(40)
    public void saxon6Transformer() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(SAXON6_APP.getName());
        Assert.assertEquals("com.icl.saxon.Controller", actual);
    }

    @Test
    @RunAsClient
    @InSequence(41)
    public void saxon6TransformerExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(SAXON6_APP.getName(), "com.icl.saxon.TransformerFactoryImpl");
        Assert.assertEquals("com.icl.saxon.Controller", actual);
    }

    @Test
    @RunAsClient
    @InSequence(42)
    public void saxon6SAXParser() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(SAXON6_APP.getName(), Factory.SAXParser);
        Assert.assertEquals("com.icl.saxon.aelfred.SAXParserImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(43)
    public void saxon6SAXParserExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(SAXON6_APP.getName(), Factory.SAXParser, "com.icl.saxon.aelfred.SAXParserFactoryImpl");
        Assert.assertEquals("com.icl.saxon.aelfred.SAXParserImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(44)
    public void saxon6SAXTransformer() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(SAXON6_APP.getName(), Factory.SAXTransformer);
        Assert.assertEquals("com.icl.saxon.IdentityTransformer", actual);
    }

    @Test
    @RunAsClient
    @InSequence(50)
    public void saxon9Transformer() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(SAXON9_APP.getName());
        Assert.assertEquals("net.sf.saxon.jaxp.TransformerImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(51)
    public void saxon9TransformerExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(SAXON9_APP.getName(), "net.sf.saxon.TransformerFactoryImpl");
        Assert.assertEquals("net.sf.saxon.jaxp.TransformerImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(51)
    public void saxon9SAXTransformer() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(SAXON9_APP.getName(), Factory.SAXTransformer);
        Assert.assertEquals("net.sf.saxon.jaxp.IdentityTransformer", actual);
    }

    @Test
    @RunAsClient
    @InSequence(0)
    public void xalanTransformer() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(XALAN_APP.getName());
        Assert.assertEquals("org.apache.xalan.transformer.TransformerImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void xalanTransformerExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getTransformer(XALAN_APP.getName(), "org.apache.xalan.transformer.TransformerFactoryImpl");
        Assert.assertEquals("status 500", actual);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void xalanXPath() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XALAN_APP.getName(), Factory.XPath);
        Assert.assertEquals("org.apache.xpath.jaxp.XPathImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void xalanXPathExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XALAN_APP.getName(), Factory.XPath, "org.apache.xpath.jaxp.XPathFactoryImpl");
        Assert.assertEquals("org.apache.xpath.jaxp.XPathImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(61)
    public void xercesDatatype() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.Datatype);
        Assert.assertEquals("org.apache.xerces.jaxp.datatype.XMLGregorianCalendarImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(62)
    public void xercesDatatypeExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.Datatype, "org.apache.xerces.jaxp.datatype.DatatypeFactoryImpl");
        Assert.assertEquals("org.apache.xerces.jaxp.datatype.XMLGregorianCalendarImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(63)
    public void xercesDocumentBuilder() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.DocumentBuilder);
        Assert.assertEquals("org.apache.xerces.jaxp.DocumentBuilderImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(64)
    public void xercesDocumentBuilderExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.DocumentBuilder, "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        Assert.assertEquals("org.apache.xerces.jaxp.DocumentBuilderImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(65)
    public void xercesSAXParser() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.SAXParser);
        Assert.assertEquals("org.apache.xerces.jaxp.SAXParserImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(66)
    public void xercesSAXParserExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.SAXParser, "org.apache.xerces.jaxp.SAXParserFactoryImpl");
        Assert.assertEquals("org.apache.xerces.jaxp.SAXParserImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(67)
    public void xercesSchema() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.Schema);
        Assert.assertEquals("org.apache.xerces.jaxp.validation.SimpleXMLSchema", actual);
    }

    @Test
    @RunAsClient
    @InSequence(68)
    public void xercesSchemaExplicitFactory() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.Schema, "org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        Assert.assertEquals("org.apache.xerces.jaxp.validation.SimpleXMLSchema", actual);
    }

    @Test
    @RunAsClient
    @InSequence(69)
    public void xercesXMLEvent() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.XMLEvent);
        Assert.assertEquals("org.codehaus.stax2.ri.evt.StartDocumentEventImpl", actual);
    }

    @Test
    @RunAsClient
    @InSequence(70)
    public void xercesXMLReader() throws IOException, TransformerException {
        final String actual = JaxpTestUtils.getFactory(XERCES_APP.getName(), Factory.XMLReader);
        Assert.assertEquals("org.apache.xerces.parsers.AbstractSAXParser$AttributesProxy", actual);
    }

}
