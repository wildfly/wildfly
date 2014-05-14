/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.interfaces;

import java.util.Set;

import org.jboss.as.controller.logging.ControllerLogger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class CriteriaValidator {
    final Set<InterfaceCriteria> criteria;

    CriteriaValidator(Set<InterfaceCriteria> criteria) {
        this.criteria = criteria;
    }

    String validate() {
        for (InterfaceCriteria current : criteria) {
            Validation validation = getValidation(current);
            if (validation == null) {
                continue;
            }
            for (InterfaceCriteria candidate : criteria) {
                if (current == candidate) {
                    continue;
                }
                String error = validation.validate(current, candidate);
                if (error != null) {
                    return error;
                }
            }
        }
        return null;
    }

    Validation getValidation(InterfaceCriteria criteria) {
        if (criteria instanceof LoopbackInterfaceCriteria) {
            return LOOPBACK_INTERFACE;
        } else if (criteria instanceof LinkLocalInterfaceCriteria) {
            //TODO
            //return LINK_LOCAL_INTERFACE;
            return null;
        } else if (criteria instanceof NotInterfaceCriteria) {
            return NOT_INTERFACE;
        }
        return null;
    }


    interface Validation {
        String validate(InterfaceCriteria current, InterfaceCriteria candidate);
    }

    static Validation LOOPBACK_INTERFACE = new Validation() {
        @Override
        public String validate(InterfaceCriteria current, InterfaceCriteria candidate) {
            if (candidate instanceof InetAddressMatchInterfaceCriteria) {
                return ControllerLogger.ROOT_LOGGER.cantHaveBothLoopbackAndInetAddressCriteria();
            }
            return null;
        }
    };

    //TODO This needs to check the inet address match interface criteria is not link local
//    static Validation LINK_LOCAL_INTERFACE = new Validation() {
//
//        @Override
//        public String validate(InterfaceCriteria current, InterfaceCriteria candidate) {
//            if (candidate instanceof InetAddressMatchInterfaceCriteria) {
//                return MESSAGES.cantHaveBothLinkLocalAndInetAddressCriteria();
//            }
//            return null;
//        }
//    };

    static Validation NOT_INTERFACE = new Validation() {
        @Override
        public String validate(InterfaceCriteria current, InterfaceCriteria candidate) {
            for (InterfaceCriteria curr : ((NotInterfaceCriteria)current).getAllCriteria()) {
                if (curr.equals(candidate)) {
                    return ControllerLogger.ROOT_LOGGER.cantHaveSameCriteriaForBothNotAndInclusion(candidate);
                }
            }
            return null;
        }
    };
}
