from __future__ import with_statement

import os

from sos.plugins import UbuntuPlugin, IndependentPlugin
from sos.policies.debian import DebianPolicy

class UbuntuPolicy(DebianPolicy):
    def __init__(self):
        super(UbuntuPolicy, self).__init__()
        self.distro = "Ubuntu"
        self.valid_subclasses = [UbuntuPlugin]

    @classmethod
    def check(self):
        """This method checks to see if we are running on Ubuntu.
           It returns True or False."""
        try:
            with open('/etc/lsb-release', 'r') as fp:
                return "Ubuntu" in fp.read()
        except:
            return False
