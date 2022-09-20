/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.spec.api.test;

import jakarta.annotation.*;
import jakarta.annotation.security.*;
import jakarta.annotation.sql.*;
import jakarta.batch.api.*;
import jakarta.batch.api.chunk.*;
import jakarta.batch.api.chunk.listener.*;
import jakarta.batch.api.listener.*;
import jakarta.batch.api.partition.*;
import jakarta.batch.operations.*;
import jakarta.batch.runtime.*;
import jakarta.batch.runtime.context.*;
import jakarta.decorator.*;
import jakarta.ejb.*;
import jakarta.ejb.embeddable.*;
import jakarta.ejb.spi.*;
import jakarta.el.*;
import jakarta.enterprise.concurrent.*;
import jakarta.enterprise.context.*;
import jakarta.enterprise.context.spi.*;
import jakarta.enterprise.event.*;
import jakarta.enterprise.inject.*;
import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.lang.model.*;
import jakarta.enterprise.util.*;
import jakarta.faces.*;
import jakarta.faces.application.*;
import jakarta.faces.component.*;
import jakarta.faces.component.behavior.*;
import jakarta.faces.component.html.*;
import jakarta.faces.component.visit.*;
import jakarta.faces.context.*;
import jakarta.faces.convert.*;
import jakarta.faces.el.*;
import jakarta.faces.event.*;
import jakarta.faces.flow.*;
import jakarta.faces.lifecycle.*;
import jakarta.faces.model.*;
import jakarta.faces.render.*;
import jakarta.faces.validator.*;
import jakarta.faces.view.*;
import jakarta.faces.view.facelets.*;
import jakarta.faces.webapp.*;
import jakarta.inject.*;
import jakarta.interceptor.*;
import jakarta.jms.*;
import jakarta.json.*;
import jakarta.json.spi.*;
import jakarta.json.stream.*;
import jakarta.jws.*;
import jakarta.jws.soap.*;
import jakarta.mail.*;
import jakarta.mail.event.*;
import jakarta.mail.internet.*;
import jakarta.mail.search.*;
import jakarta.mail.util.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.*;
import jakarta.persistence.spi.*;
import jakarta.resource.*;
import jakarta.resource.cci.*;
import jakarta.resource.spi.*;
import jakarta.resource.spi.endpoint.*;
import jakarta.resource.spi.security.*;
import jakarta.resource.spi.work.*;
import jakarta.security.auth.message.*;
import jakarta.security.auth.message.callback.*;
import jakarta.security.auth.message.config.*;
import jakarta.security.auth.message.module.*;
import jakarta.security.jacc.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.descriptor.*;
import jakarta.servlet.http.*;
import jakarta.servlet.jsp.*;
import jakarta.servlet.jsp.el.*;
import jakarta.servlet.jsp.jstl.core.*;
import jakarta.servlet.jsp.jstl.fmt.*;
import jakarta.servlet.jsp.jstl.sql.*;
import jakarta.servlet.jsp.jstl.tlv.*;
import jakarta.servlet.jsp.tagext.*;
import jakarta.transaction.*;
import jakarta.validation.*;
import jakarta.validation.bootstrap.*;
import jakarta.validation.constraints.*;
import jakarta.validation.constraintvalidation.*;
import jakarta.validation.executable.*;
import jakarta.validation.groups.*;
import jakarta.validation.metadata.*;
import jakarta.validation.spi.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import jakarta.xml.bind.*;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.*;
import jakarta.xml.bind.attachment.*;
import jakarta.xml.bind.helpers.*;
import jakarta.xml.bind.util.*;
import jakarta.xml.soap.*;
import jakarta.xml.ws.*;
import jakarta.xml.ws.handler.*;
import jakarta.xml.ws.handler.soap.*;
import jakarta.xml.ws.http.*;
import jakarta.xml.ws.soap.*;
import jakarta.xml.ws.spi.*;
import jakarta.xml.ws.spi.http.*;
import jakarta.xml.ws.wsaddressing.*;

/**
 * This class checks the dependencies as exported by the
 * "spec-api" module to ensure that compilation of all Jakarta EE
 * 7 Specification Platform APIs are reachable.  As such, no runtime
 * assertions are required here, only references.  If this class compiles, 
 * all noted references are reachable within the spec-api dependency chain. 
 * 
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @see http://docs.oracle.com/javaee/7/api/
 */
@SuppressWarnings("unused")
public class SpecApiDependencyCheck {

    
    
}
