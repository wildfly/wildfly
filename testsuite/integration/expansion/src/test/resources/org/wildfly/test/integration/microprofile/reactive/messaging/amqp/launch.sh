#!/bin/sh

#
# Copyright The WildFly Authors
# SPDX-License-Identifier: Apache-2.0
#

# This is a copy of the launch script used for the container with some additions to
# be able to use our own broker.xml. The main purpose is to be able to configure SSL with
# a passed in keystore

set -e

if [ "${SCRIPT_DEBUG}" = "true" ] ; then
    set -x
    echo "Script debugging is enabled, allowing bash commands and their arguments to be printed as they are executed"
fi

export BROKER_IP=`hostname -I | cut -f 1 -d ' '`

function configure() {

    export CONTAINER_ID=$HOSTNAME

    if [ ! -d "broker" -o "$AMQ_RESET_CONFIG" = "true" ]; then
        AMQ_ARGS="--role $AMQ_ROLE --name $AMQ_NAME --allow-anonymous --http-host $BROKER_IP --host $BROKER_IP "
    	  if [ -n "${AMQ_USER}" -a -n "${AMQ_PASSWORD}" ] ; then
			      AMQ_ARGS="--user $AMQ_USER --password $AMQ_PASSWORD $AMQ_ARGS "
		    fi
        if [ "$AMQ_CLUSTERED" = "true" ]; then
            echo "Broker will be clustered"
            AMQ_ARGS="$AMQ_ARGS --clustered --cluster-user $AMQ_CLUSTER_USER --cluster-password $AMQ_CLUSTER_PASSWORD"
        fi
        if [ "$AMQ_RESET_CONFIG" ]; then
            AMQ_ARGS="$AMQ_ARGS --force"
        fi
        if [ "$AMQ_EXTRA_ARGS" ]; then
            AMQ_ARGS="$AMQ_ARGS $AMQ_EXTRA_ARGS"
        fi

        PRINT_ARGS="${AMQ_ARGS/--password $AMQ_PASSWORD/--password XXXXX}"
        PRINT_ARGS="${PRINT_ARGS/--user $AMQ_USER/--user XXXXX}"
        PRINT_ARGS="${PRINT_ARGS/--cluster-user $AMQ_CLUSTER_USER/--cluster-user XXXXX}"
        PRINT_ARGS="${PRINT_ARGS/--cluster-password $AMQ_CLUSTER_PASSWORD/--cluster-password XXXXX}"
        PRINT_ARGS="${PRINT_ARGS/--ssl-key-password $AMQ_KEYSTORE_PASSWORD/--ssl-key-password XXXXX}"
        PRINT_ARGS="${PRINT_ARGS/--ssl-trust-password $AMQ_TRUSTSTORE_PASSWORD/--ssl-trust-password XXXXX}"

        echo "Creating Broker with args $PRINT_ARGS"
        $AMQ_HOME/bin/artemis create broker $AMQ_ARGS
    fi

}

function runServer() {
  configure

  ##########################################
  # ADDED CODE
  cp ~/config/broker.xml ~/broker/etc/broker.xml
  # END - ADDED CODE
  ##########################################

  echo "Running Broker"
  exec ~/broker/bin/artemis run
}

runServer