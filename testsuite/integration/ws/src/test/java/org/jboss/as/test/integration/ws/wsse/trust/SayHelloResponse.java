/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "sayHelloResponse", namespace = "http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sayHelloResponse", namespace = "http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy")
public class SayHelloResponse {

    @XmlElement(name = "return", namespace = "")
    private String _return;

    public String getReturn() {
        return this._return;
    }

    public void setReturn(String _return) {
        this._return = _return;
    }

}
