
package org.jboss.as.test.integration.ws.wsrm.generated;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the org.jboss.as.test.integration.ws.wsrm.generated package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _WriteLogMessage_QNAME = new QName("http://www.jboss.org/jbossws/ws-extensions/wsrm", "writeLogMessage");
    private static final QName _SayHello_QNAME = new QName("http://www.jboss.org/jbossws/ws-extensions/wsrm", "sayHello");
    private static final QName _SayHelloResponse_QNAME = new QName("http://www.jboss.org/jbossws/ws-extensions/wsrm", "sayHelloResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jboss.as.test.integration.ws.wsrm.generated
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link WriteLogMessage }
     */
    public WriteLogMessage createWriteLogMessage() {
        return new WriteLogMessage();
    }

    /**
     * Create an instance of {@link SayHelloResponse }
     */
    public SayHelloResponse createSayHelloResponse() {
        return new SayHelloResponse();
    }

    /**
     * Create an instance of {@link SayHello }
     */
    public SayHello createSayHello() {
        return new SayHello();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WriteLogMessage }{@code >}}
     */
    @XmlElementDecl(namespace = "http://www.jboss.org/jbossws/ws-extensions/wsrm", name = "writeLogMessage")
    public JAXBElement<WriteLogMessage> createWriteLogMessage(WriteLogMessage value) {
        return new JAXBElement<WriteLogMessage>(_WriteLogMessage_QNAME, WriteLogMessage.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHello }{@code >}}
     */
    @XmlElementDecl(namespace = "http://www.jboss.org/jbossws/ws-extensions/wsrm", name = "sayHello")
    public JAXBElement<SayHello> createSayHello(SayHello value) {
        return new JAXBElement<SayHello>(_SayHello_QNAME, SayHello.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://www.jboss.org/jbossws/ws-extensions/wsrm", name = "sayHelloResponse")
    public JAXBElement<SayHelloResponse> createSayHelloResponse(SayHelloResponse value) {
        return new JAXBElement<SayHelloResponse>(_SayHelloResponse_QNAME, SayHelloResponse.class, null, value);
    }

}
