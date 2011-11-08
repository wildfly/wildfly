/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.service;

import org.apache.juddi.IRegistry;
import org.apache.juddi.datatype.RegistryObject;
import org.apache.juddi.datatype.response.DispositionReport;
import org.apache.juddi.datatype.response.ErrInfo;
import org.apache.juddi.datatype.response.Result;
import org.apache.juddi.error.BusyException;
import org.apache.juddi.error.FatalErrorException;
import org.apache.juddi.error.RegistryException;
import org.apache.juddi.error.UnsupportedException;
import org.apache.juddi.handler.HandlerMaker;
import org.apache.juddi.handler.IHandler;
import org.apache.juddi.registry.RegistryEngine;
import org.apache.juddi.registry.RegistryServlet;
import org.apache.juddi.util.Config;
import org.apache.juddi.util.xml.XMLUtils;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Detail;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Servlet that represents the JUDDI Registry
 * Based on the JUDDI standard servlets
 *
 * Original source at https://svn.jboss.org/repos/jbossas/projects/jaxr/tags/2.0.2
 *
 * @author Anil.Saldhana@jboss.com
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public class JUDDIServlet extends HttpServlet {

    // XML Document Builder
    private static DocumentBuilder docBuilder = null;

    // jUDDI XML Handler maker
    private static HandlerMaker maker = HandlerMaker.getInstance();

    private static Logger log = Logger.getLogger(JUDDIServlet.class);

    public void doGet(HttpServletRequest req, HttpServletResponse res)                     throws ServletException, IOException {
        res.setHeader("Allow", "POST");
        res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "The request method 'GET' is not allowed by UDDI API.");
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res)                   throws ServletException, IOException {
        res.setContentType("text/xml; charset=utf-8");

        SOAPMessage soapReq = null;
        SOAPMessage soapRes = null;

        try {
            MessageFactory msgFactory = MessageFactory.newInstance();
            soapReq = msgFactory.createMessage(null, req.getInputStream());
            soapRes = msgFactory.createMessage();
            if (log.isDebugEnabled()) {
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                soapReq.writeTo(bs);
                log.debug("Request received::" + bs.toString());
            }

            SOAPBody soapReqBody = soapReq.getSOAPBody();
            Iterator it = soapReqBody.getChildElements();
            Element uddiReq = null;
            while (uddiReq == null && it.hasNext()) {
                Object current = it.next();
                if (current instanceof Element) {
                    uddiReq = (Element) current;
                }
            }

            if (uddiReq == null)
                throw new FatalErrorException("A UDDI request was not found in the SOAP message.");

            String function = uddiReq.getLocalName();
            if ((function == null) || (function.trim().length() == 0))
                throw new FatalErrorException("The name of the UDDI request could not be identified.");
            IHandler requestHandler = maker.lookup(function);
            if (requestHandler == null)
                throw new UnsupportedException("The UDDI request " +
                        "type specified is unknown: " + function);

            String generic = uddiReq.getAttribute("generic");
            if (generic == null)
                throw new FatalErrorException("A UDDI generic attribute value was not found for UDDI request: " + function + " (The 'generic' attribute must be present)");
            else if (!generic.equals(IRegistry.UDDI_V2_GENERIC))
                throw new UnsupportedException("Currently only UDDI v2 requests are supported. The generic attribute value received was: " + generic);

            // Unmarshal the raw xml into the appropriate jUDDI
            // request object.

            RegistryObject uddiReqObj = requestHandler.unmarshal(uddiReq);
            if (uddiReqObj == null)
                throw new FatalErrorException("Uddi Request is null");

            // Grab a reference to the shared jUDDI registry
            // instance (make sure it's running) and execute the
            // requested UDDI function.

            RegistryObject uddiResObj = null;
            RegistryEngine registry = RegistryServlet.getRegistry();
            if ((registry != null) && (registry.isAvailable()))
                uddiResObj = registry.execute(uddiReqObj);
            else
                throw new BusyException("The Registry is currently unavailable.");

            // Lookup the appropriate response handler which will
            // be used to marshal the UDDI object into the appropriate
            // xml format.

            IHandler responseHandler = maker.lookup(uddiResObj.getClass().getName());
            if (responseHandler == null)
                throw new FatalErrorException("The response object type is unknown: " + uddiResObj.getClass().getName());

            // Create a new 'temp' XML element to use as a container
            // in which to marshal the UDDI response data into.

            DocumentBuilder docBuilder = getDocumentBuilder();
            Document document = docBuilder.newDocument();
            Element element = document.createElement("temp");

            // Lookup the appropriate response handler and marshal
            // the juddi object into the appropriate xml format (we
            // only support UDDI v2.0 at this time).  Attach the
            // results to the body of the SOAP response.

            responseHandler.marshal(uddiResObj, element);
            log.debug("Response that will be sent:");
            log.debug(XMLUtils.toString((Element) element.getFirstChild()));

            // Grab a reference to the 'temp' element's
            // only child here (this has the effect of
            // discarding the temp element) and append
            // this child to the soap response body

            /*document.appendChild(element.getFirstChild());
            soapRes.getSOAPBody().addDocument(document);*/
            Element el = (Element) element.getFirstChild();
            soapRes = this.createSOAPMessage(el);
        } catch (Exception ex) {
            // SOAP Fault values
            String faultCode = null;
            String faultString = null;
            String faultActor = null;

            // UDDI DispositionReport values
            String errno = null;
            String errCode = null;
            String errMsg = null;

            if (ex instanceof RegistryException) {

                log.error("RegistryException::", ex);

                RegistryException rex = (RegistryException) ex;

                faultCode = rex.getFaultCode();  // SOAP Fault faultCode
                faultString = rex.getFaultString();  // SOAP Fault faultString
                faultActor = rex.getFaultActor();  // SOAP Fault faultActor

                DispositionReport dispRpt = rex.getDispositionReport();
                if (dispRpt != null) {
                    Result result = null;
                    ErrInfo errInfo = null;

                    Vector results = dispRpt.getResultVector();
                    if ((results != null) && (!results.isEmpty()))
                        result = (Result) results.elementAt(0);

                    if (result != null) {
                        errno = String.valueOf(result.getErrno());  // UDDI Result errno
                        errInfo = result.getErrInfo();

                        if (errInfo != null) {
                            errCode = errInfo.getErrCode();  // UDDI ErrInfo errCode
                            errMsg = errInfo.getErrMsg();  // UDDI ErrInfo errMsg
                        }
                    }
                }
            } else {

                log.error(ex.getMessage(), ex);

                faultCode = "Server";
                faultString = ex.getMessage();
                faultActor = null;


                errno = String.valueOf(Result.E_FATAL_ERROR);
                errCode = Result.lookupErrCode(Result.E_FATAL_ERROR);
                errMsg = Result.lookupErrText(Result.E_FATAL_ERROR);
            }

            try {
                SOAPBody soapResBody = soapRes.getSOAPBody();
                SOAPFault soapFault = soapResBody.addFault();
                if (faultCode == null)
                    faultCode = "Unavailable";

                if (faultCode.contains(":") == false) {
                    faultCode = ((Node) soapResBody).getPrefix() + ":" + faultCode;
                }

                soapFault.setFaultCode(faultCode);
                soapFault.setFaultString(faultString);
                soapFault.setFaultActor(faultActor);

                Detail faultDetail = soapFault.addDetail();

                SOAPElement dispRpt = faultDetail.addChildElement("dispositionReport", "", IRegistry.UDDI_V2_NAMESPACE);
                dispRpt.setAttribute("generic", IRegistry.UDDI_V2_GENERIC);
                dispRpt.setAttribute("operator", Config.getOperator());

                SOAPElement result = dispRpt.addChildElement("result");
                result.setAttribute("errno", errno);

                SOAPElement errInfo = result.addChildElement("errInfo");
                errInfo.setAttribute("errCode", errCode);
                errInfo.setValue(errMsg);
            } catch (Exception e) { // if we end up in here it's just NOT good.
                log.error("A serious error has occured while assembling the SOAP Fault.", e);
            }
        } finally {
            try {
                if (log.isDebugEnabled()) {
                    ByteArrayOutputStream bs = new ByteArrayOutputStream();
                    soapRes.writeTo(bs);
                    log.debug("Response being sent::" + bs.toString());
                }
                soapRes.writeTo(res.getOutputStream());
            } catch (SOAPException sex) {
                log.error("SOAPException::", sex);
            }
        }
    }

    /**
     *
     */
    private DocumentBuilder getDocumentBuilder() {
        if (docBuilder == null)
            docBuilder = createDocumentBuilder();
        return docBuilder;
    }

    /**
     *
     */
    private synchronized DocumentBuilder createDocumentBuilder() {
        if (docBuilder != null)
            return docBuilder;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            //factory.setValidating(true);

            docBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pcex) {
            log.error("ParserConfigurationException::", pcex);
        }

        return docBuilder;
    }

    private SOAPMessage createSOAPMessage(Element elem) throws Exception {
        String prefix = "uddi";
        MessageFactory msgFactory = MessageFactory.newInstance();
        SOAPFactory factory = SOAPFactory.newInstance();

        SOAPMessage message = msgFactory.createMessage();
        message.getSOAPHeader().detachNode();
        SOAPPart soapPart = message.getSOAPPart();
        SOAPBody soapBody = soapPart.getEnvelope().getBody();
        //Create the outer body element
        String uddins = IRegistry.UDDI_V2_NAMESPACE;
        Name bodyName = factory.createName(elem.getNodeName(), prefix, uddins);
        SOAPBodyElement bodyElement = soapBody.addBodyElement(bodyName);
        bodyElement.addNamespaceDeclaration(prefix, uddins);
        appendAttributes(bodyElement, elem.getAttributes(), factory);
        appendElements(bodyElement, elem.getChildNodes(), factory);
        return message;
    }

    private void appendAttributes(SOAPElement bodyElement, NamedNodeMap nnm,
                                  SOAPFactory factory) throws SOAPException {
        int len = nnm != null ? nnm.getLength() : 0;
        for (int i = 0; i < len; i++) {
            Node n = nnm.item(i);
            String nodename = n.getNodeName();
            String nodevalue = n.getNodeValue();
            if ("xmlns".equals(nodename))
                continue;
            //Special case:  xml:lang
            if ("xml:lang".equals(nodename)) {
                Name xmlLang = factory.createName("lang", "xml", "");
                bodyElement.addAttribute(xmlLang, nodevalue);
            } else
                bodyElement.addAttribute(factory.createName(nodename), nodevalue);
        }
    }

    private void appendElements(SOAPElement bodyElement, NodeList nlist,
                                SOAPFactory factory)
            throws SOAPException {
        String prefix = "uddi";
        String uddins = IRegistry.UDDI_V2_NAMESPACE;
        int len = nlist != null ? nlist.getLength() : 0;

        for (int i = 0; i < len; i++) {
            Node node = nlist.item(i);
            short nodeType = node != null ? node.getNodeType() : -100;
            if (Node.ELEMENT_NODE == nodeType) {
                Element el = (Element) node;
                Name name = factory.createName(el.getNodeName(), prefix, uddins);
                SOAPElement attachedEl = bodyElement.addChildElement(name);
                appendAttributes(attachedEl, el.getAttributes(), factory);
                appendElements(attachedEl, el.getChildNodes(), factory);
            } else if (nodeType == Node.TEXT_NODE) {
                bodyElement.addTextNode(node.getNodeValue());
            }
        }
    }
}
