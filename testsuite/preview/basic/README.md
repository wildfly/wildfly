WildFly Preview Basic Testsuite
===============================

This test suite use TestContainers.
On Linux you might be using podman instead of docker.
For those tests to run properly you need to follow these instructions (taken from https://quarkus.io/blog/quarkus-devservices-testcontainers-podman/)

`
    # Install the required podman packages from dnf. If you're not using rpm based distro, replace with respective package manager
    sudo dnf install podman podman-docker
    # Enable the podman socket with Docker REST API
    systemctl --user enable podman.socket --now
    # Set the required envvars
    export DOCKER_HOST=unix:///run/user/${UID}/podman/podman.sock
    export TESTCONTAINERS_RYUK_DISABLED=true
`

