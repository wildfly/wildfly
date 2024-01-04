/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "sayHello", namespace = "http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sayHello", namespace = "http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy")
public class SayHello {
}
