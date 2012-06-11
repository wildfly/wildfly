/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.logging.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class ModelMatcher {
    static class Model {
        private PathAddress address;
        private List<String> attributes = new LinkedList<String>();
        private List<Model> children = new LinkedList<Model>();

        Model(PathAddress address) {
            this.address = address;
        }

        protected void addAttribute(String name) {
            attributes.add(name);
        }

        protected void addChild(Model child) {
            children.add(child);
        }


        public boolean isSame(Model model) {
            if (this == model) { return true; }


            if (address != null ? !address.equals(model.address) : model.address != null) { return false; }
            if (attributes != null ? !attributes.equals(model.attributes) : model.attributes != null) { return false; }
            if (children != null ? !children.equals(model.children) : model.children != null) { return false; }

            return true;
        }

        public int getAttributeCount() {
            int i = attributes.size();
            List<Model> c = children;
            while (c != null && !c.isEmpty()) {
                for (Model child : c) {
                    i += child.attributes.size();
                    c = child.children;
                }
            }
            return i;
        }

        /**
         * used only when both source and target are same
         *
         * @return return simple 1=1 rules
         */
        public List<TransformRule> toRules() {
            LinkedList<TransformRule> rules = new LinkedList<TransformRule>();
            for (String attribute : attributes) {
                rules.add(new TransformRule(address, address, attribute, attribute));
            }
            List<Model> c = children;
            while (c != null && !c.isEmpty()) {
                for (Model child : c) {
                    for (String attribute : child.attributes) {
                        rules.add(new TransformRule(child.address, child.address, attribute, attribute));
                    }
                    c = child.children;
                }
            }
            return rules;
        }

        public List<TransformRule> getMatchedRules(Model other) {
            LinkedList<TransformRule> rules = new LinkedList<TransformRule>();
            for (String attribute : attributes) {
                TransformRule r = matchAttribute(attribute, other);
                if (r != null) {
                    r.setSourceAddress(address);
                    rules.add(r);
                }

            }
            List<Model> c = children;
            while (c != null && !c.isEmpty()) {
                for (Model child : c) {
                    for (String attribute : child.attributes) {
                        TransformRule r = matchAttribute(attribute, child);
                        if (r != null) {
                            r.setSourceAddress(child.address);
                            rules.add(r);
                        }
                    }
                    c = child.children;
                }
            }
            return rules;
        }

        private TransformRule matchAttribute(final String source, Model other) {
            for (String attribute : other.attributes) {
                if (attribute.equals(source)) {
                    return new TransformRule(null, other.address, source, attribute);
                }

            }
            List<Model> c = other.children;
            while (c != null && !c.isEmpty()) {
                for (Model child : c) {
                    for (String attribute : child.attributes) {
                        if (attribute.equals(source)) {
                            return new TransformRule(null, child.address, source, attribute);
                        }
                    }
                    c = child.children;
                }
            }
            return null;
        }
    }


    public static List<TransformRule> getRules(final ImmutableManagementResourceRegistration current, ImmutableManagementResourceRegistration target) {
        Model currentAttributes = readModel(current, PathAddress.EMPTY_ADDRESS);
        Model targetAttributes = readModel(target, PathAddress.EMPTY_ADDRESS);
        if (targetAttributes.isSame(currentAttributes)) {
            return targetAttributes.toRules();
        }
        return targetAttributes.getMatchedRules(currentAttributes);
    }


    public static Model readModel(final ImmutableManagementResourceRegistration def, final PathAddress address) {
        Model res = new Model(address);
        Set<String> attributesList = def.getAttributeNames(PathAddress.EMPTY_ADDRESS);

        for (String name : attributesList) {
            res.addAttribute(name);
        }
        for (PathElement element : def.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
            final ImmutableManagementResourceRegistration child = def.getSubModel(PathAddress.pathAddress(element));
            if (child != null) {
                res.addChild(readModel(child, address.append(element)));
            }
        }
        return res;
    }


}
