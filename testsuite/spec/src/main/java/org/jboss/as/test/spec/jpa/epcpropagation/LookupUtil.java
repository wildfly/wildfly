/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.spec.jpa.epcpropagation;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Simple lookup utility
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
final class LookupUtil {

    private LookupUtil() {
        throw new UnsupportedOperationException("No instances permitted");
    }

    static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        // The String "epc-propagation" here refers to the deployed archive name; if this changes in the test
        // then it'll need to be changed here
        return interfaceType.cast(new InitialContext().lookup("java:global/" + "epc-propagation" + "/" + beanName + "!"
                + interfaceType.getName()));
    }

}
