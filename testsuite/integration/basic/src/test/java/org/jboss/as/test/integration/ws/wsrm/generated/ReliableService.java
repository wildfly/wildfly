package org.jboss.as.test.integration.ws.wsrm.generated;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by Apache CXF 2.4.6
 * 2012-02-16T17:56:55.227+01:00
 * Generated source version: 2.4.6
 * 
 */
@WebService(targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wsrm", name = "ReliableService")
@XmlSeeAlso({ObjectFactory.class})
public interface ReliableService {

    @Oneway
    @RequestWrapper(localName = "writeLogMessage", targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wsrm", className = "org.jboss.as.test.integration.ws.wsrm.generated.WriteLogMessage")
    @WebMethod
    public void writeLogMessage();

    @WebResult(name = "return", targetNamespace = "")
    @RequestWrapper(localName = "sayHello", targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wsrm", className = "org.jboss.as.test.integration.ws.wsrm.generated.SayHello")
    @WebMethod
    @ResponseWrapper(localName = "sayHelloResponse", targetNamespace = "http://www.jboss.org/jbossws/ws-extensions/wsrm", className = "org.jboss.as.test.integration.ws.wsrm.generated.SayHelloResponse")
    public java.lang.String sayHello(
        @WebParam(name = "arg0", targetNamespace = "")
        java.lang.String arg0
    );
}
