
package org.jboss.as.test.integration.ws.resourcename.generated;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.jboss.as.test.integration.ws.resourcename.generated package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SomeFeature_QNAME = new QName("http://www.test.nl/xsd/test/messages", "SomeFeature");
    private final static QName _ResponseMessage_QNAME = new QName("http://www.test.nl/xsd/test/messages", "responseMessage");
    private final static QName _RequestMessage_QNAME = new QName("http://www.test.nl/xsd/test/messages", "requestMessage");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jboss.as.test.integration.ws.resourcename.generated
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ResponseType }
     * 
     */
    public ResponseType createResponseType() {
        return new ResponseType();
    }

    /**
     * Create an instance of {@link RequestType }
     * 
     */
    public RequestType createRequestType() {
        return new RequestType();
    }

    /**
     * Create an instance of {@link SomeFeatureType }
     * 
     */
    public SomeFeatureType createSomeFeatureType() {
        return new SomeFeatureType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SomeFeatureType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.test.nl/xsd/test/messages", name = "SomeFeature")
    public JAXBElement<SomeFeatureType> createSomeFeature(SomeFeatureType value) {
        return new JAXBElement<SomeFeatureType>(_SomeFeature_QNAME, SomeFeatureType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.test.nl/xsd/test/messages", name = "responseMessage")
    public JAXBElement<ResponseType> createResponseMessage(ResponseType value) {
        return new JAXBElement<ResponseType>(_ResponseMessage_QNAME, ResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.test.nl/xsd/test/messages", name = "requestMessage")
    public JAXBElement<RequestType> createRequestMessage(RequestType value) {
        return new JAXBElement<RequestType>(_RequestMessage_QNAME, RequestType.class, null, value);
    }

}
