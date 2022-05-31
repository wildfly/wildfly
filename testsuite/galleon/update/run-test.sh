#!/bin/bash
set -e

if [ -z "${WF_BASE_VERSION}" ]; then
  echo "WF_BASE_VERSION must be set to a final version."
  exit 1
fi
if [ -z "${WILDFLY_PRODUCER}" ]; then
  echo "WILDFLY_PRODUCER must be set to a valid WildFly producer."
  exit 1
fi
if [ -z "${BASE_DIR}" ]; then
  echo "BASE_DIR must be set to the test base dir"
  exit 1
fi
if [ -z "${MAVEN_LOCAL_REPO}" ]; then
  echo "MAVEN_LOCAL_REPO must be set to the maven local cache absolute directory."
  exit 1
fi

echo "Using ${MAVEN_LOCAL_REPO} as Maven local cache"

"${BASE_DIR}"/target/galleon/galleon-${GALLEON_VERSION}/bin/galleon.sh maven set-local-repository "${MAVEN_LOCAL_REPO}"

# Full install update
echo "Testing update of ${WILDFLY_PRODUCER} from ${WF_BASE_VERSION} to ${WILDFLY_PRODUCER} latest SNAPSHOT"
wildflyDir="${BASE_DIR}"/target/${WILDFLY_PRODUCER}
rm -rf "${wildflyDir}"

"${BASE_DIR}"/target/galleon/galleon-${GALLEON_VERSION}/bin/galleon.sh install ${WILDFLY_PRODUCER}:current/snapshot#${WF_BASE_VERSION} \
--dir="${wildflyDir}"
"${BASE_DIR}"/target/galleon/galleon-${GALLEON_VERSION}/bin/galleon.sh update --yes --dir="${wildflyDir}"
"${BASE_DIR}"/target/galleon/galleon-${GALLEON_VERSION}/bin/galleon.sh get-info --type=configs --dir="${wildflyDir}"
rm -rf "${wildflyDir}"

echo "Testing update of ${WILDFLY_PRODUCER} with Galleon layers from ${WF_BASE_VERSION} to ${WILDFLY_PRODUCER} latest SNAPSHOT"
# Layers base update
wildflyLayersDir="${BASE_DIR}"/target/${WILDFLY_PRODUCER}-layers
rm -rf "${wildflyLayersDir}"
"${BASE_DIR}"/target/galleon/galleon-${GALLEON_VERSION}/bin/galleon.sh install ${WILDFLY_PRODUCER}:current/snapshot#${WF_BASE_VERSION} \
--dir="${wildflyLayersDir}" --layers=${GALLEON_LAYERS}
"${BASE_DIR}"/target/galleon/galleon-${GALLEON_VERSION}/bin/galleon.sh update --yes --dir="${wildflyLayersDir}"
"${BASE_DIR}"/target/galleon/galleon-${GALLEON_VERSION}/bin/galleon.sh get-info --type=configs --dir="${wildflyLayersDir}"
rm -rf "${wildflyLayersDir}"