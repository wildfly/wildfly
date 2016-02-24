git submodule update --init --remote
cd ../testsuite/additional-testsuite/eap-additional-testsuite
git fetch origin refs/pull/17/head && git checkout FETCH_HEAD
echo "Git Submodule update is done..."
