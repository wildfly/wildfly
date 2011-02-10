/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.test.embedded.domain.xml;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * A XSDValidationUnitTestCase.
 *
 * @author Brian Stansberry
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@Ignore
public class StandardConfigsXMLParsingMarshallingUnitTestCase {

//    @Deployment
//    public static Archive<?> getDeployment(){
//        return ShrinkWrapUtils.createJavaArchive("domain-xml/standard-marshalling.jar", StandardConfigsXMLParsingMarshallingUnitTestCase.class);
//    }
//
//    @Test
//    public void testHost() throws Exception {
//        URL url = getXmlUrl("domain/configuration/host.xml");
//        InputStream reader = getReader(url);
//        HostModel model = parseHost(reader);
//        String xml = writeModel(Element.HOST, model);
//        reader = new ByteArrayInputStream(xml.getBytes());
//        parseHost(reader);
//    }
//
//    @Test
//    public void testDomain() throws Exception {
//        URL url = getXmlUrl("domain/configuration/domain.xml");
//        InputStream reader = getReader(url);
//        DomainModel model = parseDomain(reader);
//        String xml = writeModel(Element.DOMAIN, model);
//        reader = new ByteArrayInputStream(xml.getBytes());
//        parseDomain(reader);
//    }
//
//    @Test
//    public void testStandalone() throws Exception {
//        URL url = getXmlUrl("standalone/configuration/standalone.xml");
//        InputStream reader = getReader(url);
//        ServerModel model = parseServer(reader);
//        String xml = writeModel(Element.SERVER, model);
//        reader = new ByteArrayInputStream(xml.getBytes());
//        parseServer(reader);
//    }
//
//    private DomainModel parseDomain(final InputStream reader) throws ModuleLoadException {
//        final XMLMapper mapper = XMLMapper.Factory.create();
//        registerStandardDomainReaders(mapper);
//        try {
//            final List<AbstractDomainModelUpdate<?>> domainUpdates = new ArrayList<AbstractDomainModelUpdate<?>>();
//            mapper.parseDocument(domainUpdates, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedInputStream(reader)));
//            final DomainModel domainModel = new DomainModel();
//            for(final AbstractDomainModelUpdate<?> update : domainUpdates) {
//                domainModel.update(update);
//            }
//            return domainModel;
//        } catch (RuntimeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new RuntimeException("Caught exception during processing of domain.xml", e);
//        }
//    }
//
//    private HostModel parseHost(final InputStream reader) throws ModuleLoadException {
//        final XMLMapper mapper = XMLMapper.Factory.create();
//        registerStandardHostReaders(mapper);
//        try {
//            final List<AbstractHostModelUpdate<?>> hostUpdates = new ArrayList<AbstractHostModelUpdate<?>>();
//            mapper.parseDocument(hostUpdates, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedInputStream(reader)));
//            final HostModel hostModel = new HostModel();
//            for(final AbstractHostModelUpdate<?> update : hostUpdates) {
//                hostModel.update(update);
//            }
//            return hostModel;
//        } catch (RuntimeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new RuntimeException("Caught exception during processing of host.xml", e);
//        }
//    }
//
//    private ServerModel parseServer(final InputStream reader) throws ModuleLoadException {
//        final XMLMapper mapper = XMLMapper.Factory.create();
//        registerStandardServerReaders(mapper);
//        try {
//            final List<AbstractServerModelUpdate<?>> serverUpdates = new ArrayList<AbstractServerModelUpdate<?>>();
//            mapper.parseDocument(serverUpdates, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedInputStream(reader)));
//            final ServerModel serverModel = new ServerModel();
//            for(final AbstractServerModelUpdate<?> update : serverUpdates) {
//                serverModel.update(update);
//            }
//            return serverModel;
//        } catch (RuntimeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new RuntimeException("Caught exception during processing of standalone.xml", e);
//        }
//    }
//
//    private String writeModel(final Element element, final XMLContentWriter content) throws Exception, FactoryConfigurationError {
//        final XMLMapper mapper = XMLMapper.Factory.create();
//        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        final BufferedOutputStream bos = new BufferedOutputStream(baos);
//        final XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(bos);
//        try {
//            mapper.deparseDocument(new RootElementWriter(element, content), xmlWriter);
//            xmlWriter.close();
//        }
//        catch (XMLStreamException e) {
//            xmlWriter.close();
//            bos.close();
//            baos.close();
//            // Dump some diagnostics
//            System.out.println("XML Content that was written prior to exception:");
//            System.out.println(new String(baos.toByteArray()));
//            throw e;
//        }
//        finally {
//            bos.close();
//            baos.close();
//        }
//        return new String(baos.toByteArray());
//    }
//
//    private synchronized void registerStandardDomainReaders(XMLMapper mapper) throws ModuleLoadException {
//        mapper.registerRootElement(new QName(Namespace.CURRENT.getUriString(), Element.DOMAIN.getLocalName()), ModelXmlParsers.DOMAIN_XML_READER);
//    }
//
//    private synchronized void registerStandardHostReaders(XMLMapper mapper) throws ModuleLoadException {
//        mapper.registerRootElement(new QName(Namespace.CURRENT.getUriString(), Element.HOST.getLocalName()), ModelXmlParsers.HOST_XML_READER);
//    }
//
//    private synchronized void registerStandardServerReaders(XMLMapper mapper) throws ModuleLoadException {
//        mapper.registerRootElement(new QName(Namespace.CURRENT.getUriString(), Element.SERVER.getLocalName()), ModelXmlParsers.SERVER_XML_READER);
//    }
//
//    private URL getXmlUrl(String xmlName) throws MalformedURLException {
//        // user.dir will point to the root of this module
//        File f = new File(getASHome());
//        f = new File(f, xmlName);
//        return f.toURI().toURL();
//    }
//
//    private InputStream getReader(URL url) throws IOException {
//        URLConnection connection = url.openConnection();
//        InputStream is = connection.getInputStream();
////        InputStreamReader isr = new InputStreamReader(is);
////        return isr;
//        return is;
//    }
//
//    private class RootElementWriter implements XMLContentWriter {
//
//        private final Element element;
//        private final XMLContentWriter content;
//
//        RootElementWriter(final Element element, final XMLContentWriter content) {
//            this.element = element;
//            this.content = content;
//        }
//
//        @Override
//        public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
//            streamWriter.writeStartDocument();
//            streamWriter.writeStartElement(element.getLocalName());
//            content.writeContent(streamWriter);
//            streamWriter.writeEndDocument();
//        }
//
//    }
//
//    private static String getASHome() {
//       File f = new File(".");
//       f = f.getAbsoluteFile();
//       while(f.getParentFile() != null) {
//          if("testsuite".equals(f.getName())) {
//             Assert.assertNotNull("Expected to find a parent directory for " + f.getAbsolutePath(), f.getParentFile());
//             f = f.getParentFile();
//             f = new File(f, "build");
//             Assert.assertTrue("The server 'build' dir exists", f.exists());
//             f = new File(f, "target");
//             File[] children = f.listFiles();
//             f = null;
//             if (children != null)
//                 for (File child : children)
//                     if (child.getName().startsWith("jboss-"))
//                         f = child;
//
//             if(f == null || !f.exists())
//                Assert.fail("The server hasn't been built yet.");
//             Assert.assertTrue("The server 'build/target' dir exists", f.exists());
//             return f.getAbsolutePath();
//          } else {
//             f = f.getParentFile();
//          }
//       }
//       return null;
//    }
}
