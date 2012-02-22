from __future__ import with_statement
from sos import _sos as _
from sos.plugins import UbuntuPlugin, IndependentPlugin
from sos.policies.debian import DebianPolicy, DebianPackageManager
from sos.utilities import shell_out

import os
        
class UbuntuPolicy(DebianPolicy):
    def __init__(self):
        super(UbuntuPolicy, self).__init__()

    def validatePlugin(self, plugin_class):
        "Checks that the plugin will execute given the environment"
        return issubclass(plugin_class, UbuntuPlugin) or issubclass(plugin_class, IndependentPlugin)

    @classmethod
    def check(self):
        """This method checks to see if we are running on Ubuntu.
           It returns True or False."""
        if os.path.isfile('/etc/lsb-release'):
            try:
                with open('/etc/lsb-release', 'r') as fp:
                    return "Ubuntu" in fp.read()
            except:
                return False
        return False

    def get_msg(self):
        msg_dict = {"distro": "Ubuntu"}
        return self.msg % msg_dict
