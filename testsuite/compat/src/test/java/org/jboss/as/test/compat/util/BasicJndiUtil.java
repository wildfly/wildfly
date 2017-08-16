/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.compat.util;

import static java.lang.String.format;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.jboss.as.test.compat.jpa.eclipselink.wildfly8954.PersistenceXmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that allows us to interact with the wildfly jndi registered objects.
 */
public class BasicJndiUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceXmlHelper.class);

    public static BasicJndiUtil SINGLETON = new BasicJndiUtil();

    private static final String JNDI_ROOT_NAME = "";
    private static final int ROOT_DEPTH_ZERO = 0;
    private static final String FOUR_SPACES_STR = "    ";

    private BasicJndiUtil() {
    }

    /**
     * Lookup an EJB deployed with an EAR.
     *
     * @param initialContext the initial context of the container where we can hunt for our deployed ejbs
     * @param earName the name of the enterprise application archieve containing the ejbs
     * @param beanName the jndi bean name (e.g. SFSB1)
     * @param interfaceType the business interface (e.g. the local or remote interface) offered by the ejb
     * @return The desired ejb if it can be found, otherwise a runtime excpetion shall be fired up
     */
    public <T> T lookupEjbWithinEar(InitialContext initialContext, String earName, String beanName, Class<T> interfaceType) {
        // (a) Setup a jndi portable name to find the deployed component
        String ejbJndiPortableName = String.format("java:global/%1$s/beans/%2$s!%3$s",
            // 1, 2, 3
            earName, beanName, interfaceType.getCanonicalName());
        try {
            // (b) Execute the jndi lookup and cast it out to the expected bean type
            return interfaceType.cast(initialContext.lookup(ejbJndiPortableName));
        } catch (Exception e) {
            // (c) Jndi has gone wrong - safely dump the jndi tree
            dumpFullJndiTree(initialContext);

            // (d) ensure that the excpetion that caused us to come here gets reported
            String errMsg = String.format("Unexpected error took place while attempting to lookup deployed ejb: %1$s. %n%n"
                + "Error was: %2$s.", ejbJndiPortableName, e.getMessage());
            throw new RuntimeException(errMsg, e);
        }
    }

    /**
     * Dumps the full jndi tree, starting from the root element.
     *
     * <P>
     * For example: <br>
     * {@code
     *  depth: [0]  jndiName: []
            depth: [1]  jndiName: [TransactionManager]
     * }
     */
    public void dumpFullJndiTree(InitialContext initialContext) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            dumpTreeEntryListRecursive(stringBuilder, initialContext, JNDI_ROOT_NAME, ROOT_DEPTH_ZERO);
            LOGGER.info("JNDI DUMP: {} ", stringBuilder.toString());
        } catch (Exception ignoreE) {
            LOGGER.error("Unexpected error took place while attempting to dump out the jndi tree: {} ", ignoreE.getMessage(), ignoreE);
        }
    }

    /**
     * Prints the current element to expand, and recursively expands all of the child jndi element names.
     *
     * @param sbuild The string builder object that is being pumped with jndi name print statements - shall be logged in the end
     * @param initialContext The initial jndi context that we use to do our jndi lookups / listings
     * @param list The list of jndi names to be expanded on the current iteration
     * @param jndiName The current jndi name to expand
     * @param currentDepth A value ranging from 0 to N, where 0 is for the root name "" and the rest N is equal to the name of /
     *        slashes in the jndi name + 1. It creases each time we go depper in the recursion.
     * @throws NamingException a jndi lookup error
     */
    private void dumpTreeEntryListRecursive(StringBuilder sbuild, InitialContext initialContext, String jndiName, int currentDepth) throws NamingException {
        // (a) Start by printing out the current jndi name being exapnded
        sbuild.append(format("%n"));
        sbuild.append(createStringWithSpaces(currentDepth));
        String currentNode = String.format("depth: [%1$s]  jndiName: [%2$s] ", currentDepth, jndiName);
        sbuild.append(currentNode);

        // (b) Load all of the child jndi names
        NamingEnumeration<NameClassPair> childJndiNames = safelyGetChildElements(initialContext, jndiName);
        if (childJndiNames == null) {
            // we have reach a leaf element, we cannot co any deeper with this name
            return;
        }

        // (c) Loop over each of the jndi child elements for the current name
        while (childJndiNames.hasMore()) {
            // (i) load the current child element
            NameClassPair currentChildJndiName = childJndiNames.next();

            // (ii) Determine what the next jndi name should be like
            String nextJndiName = JNDI_ROOT_NAME.equals(jndiName) ?
            // - This is the very first iteration when we are deling with ""
                currentChildJndiName.getName()
                // - This is the default cause when we are going deeper and deep
                : String.format("%1$s/%2$s", jndiName, currentChildJndiName.getName());

            // (iii) Go deeper with the recursion and attempt to exapnd the next jndi name
            int nextDepth = currentDepth + 1;
            dumpTreeEntryListRecursive(sbuild, initialContext, nextJndiName, nextDepth);
        }
    }

    /**
     * Not every jndi name can be listed, because not every jndi name represents a naming context. Some will correspond to non
     * listable objects. In case of for example, javax.naming.NotContextException: TransactionManager, we want to return an
     * empty list (the node is a leaf).
     *
     * @param initialContext the initial context to help us list the name
     * @param jndiNameToExpand the name we shall attempt to list
     * @return Null when the name cannot be exapnded, otherwise the leaf elements
     */
    private NamingEnumeration<NameClassPair> safelyGetChildElements(InitialContext initialContext, String jndiNameToExpand) {
        try {
            return initialContext.list(jndiNameToExpand);
        } catch (NamingException ignoreE) {
            // we do not care - most likely we are dealing with a leaf element
            return null;
        }
    }

    /**
     * Create a string that serves as identation. The string shall hold 4 * depth spaces within it.
     *
     * @param depth the depth of our recursion
     * @return a string with an appropriate number of spaces for our current recursion depth.
     */
    private String createStringWithSpaces(int depth) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            stringBuilder.append(FOUR_SPACES_STR);
        }
        return stringBuilder.toString();
    }

}
