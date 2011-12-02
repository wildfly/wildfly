"""
Gather information about a system and report it using plugins
supplied for application-specific information
"""
## sosreport.py
## gather information about a system and report it

## Copyright (C) 2006 Steve Conklin <sconklin@redhat.com>

### This program is free software; you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation; either version 2 of the License, or
## (at your option) any later version.

## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.

## You should have received a copy of the GNU General Public License
## along with this program; if not, write to the Free Software
## Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

# pylint: disable-msg = W0611
# pylint: disable-msg = W0702
# pylint: disable-msg = R0912
# pylint: disable-msg = R0914
# pylint: disable-msg = R0915
# pylint: disable-msg = R0913
# pylint: disable-msg = E0611
# pylint: disable-msg = E1101
# pylint: disable-msg = R0904
# pylint: disable-msg = R0903

import sys
import traceback
import os
import logging
from optparse import OptionParser, Option
import ConfigParser
from sos.plugins import import_plugin
from sos.utilities import ImporterHelper
from stat import ST_UID, ST_GID, ST_MODE, ST_CTIME, ST_ATIME, ST_MTIME, S_IMODE
from time import strftime, localtime
from collections import deque
from itertools import izip
import textwrap
import tempfile

from sos import _sos as _
from sos import __version__
import sos.policies
from sos.utilities import TarFileArchive, ZipFileArchive, compress
from sos.reporting import Report, Section, Command, CopiedFile, CreatedFile, Alert, Note, PlainTextReport

class TempFileUtil(object):

    def __init__(self, tmp_dir):
        self.tmp_dir = tmp_dir
        self.files = []

    def new(self):
       fd, fname = tempfile.mkstemp(dir=self.tmp_dir)
       fobj = open(fname, 'w')
       self.files.append((fname, fobj))
       return fobj

    def clean(self):
        for fname, f in self.files:
            try:
                f.flush()
                f.close()
            except Exception, e:
                pass
            try:
                os.unlink(fname)
            except Exception, e:
                pass
        self.files = []


class OptionParserExtended(OptionParser):
    """ Show examples """
    def print_help(self, out=sys.stdout):
        """ Prints help content including examples """
        OptionParser.print_help(self, out)
        print
        print "Some examples:"
        print
        print " enable cluster plugin only and collect dlm lockdumps:"
        print "   # sosreport -o cluster -k cluster.lockdump"
        print
        print " disable memory and samba plugins, turn off rpm -Va collection:"
        print "   # sosreport -n memory,samba -k rpm.rpmva=off"
        print

class SosOption(Option):
    """Allow to specify comma delimited list of plugins"""
    ACTIONS = Option.ACTIONS + ("extend",)
    STORE_ACTIONS = Option.STORE_ACTIONS + ("extend",)
    TYPED_ACTIONS = Option.TYPED_ACTIONS + ("extend",)

    def take_action(self, action, dest, opt, value, values, parser):
        """ Performs list extension on plugins """
        if action == "extend":
            try:
                lvalue = value.split(",")
            except:
                pass
            else:
                values.ensure_value(dest, deque()).extend(lvalue)
        else:
            Option.take_action(self, action, dest, opt, value, values, parser)

def parse_options(opts):
    """ Parse command line options """

    parser = OptionParserExtended(option_class=SosOption)
    parser.add_option("-l", "--list-plugins", action="store_true",
                         dest="listPlugins", default=False,
                         help="list plugins and available plugin options")
    parser.add_option("-n", "--skip-plugins", action="extend",
                         dest="noplugins", type="string",
                         help="skip these plugins", default = deque())
    parser.add_option("-e", "--enable-plugins", action="extend",
                         dest="enableplugins", type="string",
                         help="enable these plugins", default = deque())
    parser.add_option("-o", "--only-plugins", action="extend",
                         dest="onlyplugins", type="string",
                         help="enable these plugins only", default = deque())
    parser.add_option("-k", action="append",
                         dest="plugopts", type="string",
                         help="plugin options in plugname.option=value format (see -l)")
    parser.add_option("-a", "--alloptions", action="store_true",
                         dest="usealloptions", default=False,
                         help="enable all options for loaded plugins")
    parser.add_option("-u", "--upload", action="store",
                         dest="upload", default=False,
                         help="upload the report to an ftp server")
    parser.add_option("--batch", action="store_true",
                         dest="batch", default=False,
                         help="do not ask any question (batch mode)")
    parser.add_option("--no-colors", action="store_true",
                         dest="nocolors", default=False,
                         help="do not use terminal colors for text")
    parser.add_option("-v", "--verbose", action="count",
                         dest="verbosity",
                         help="increase verbosity")
    parser.add_option("", "--silent", action="store_true",
                         dest="silent", default=False,
                         help="Only display FATAL errors on stdout")
    parser.add_option("--debug", action="count",
                         dest="debug",
                         help="enabling debugging through python debugger")
    parser.add_option("--ticket-number", action="store",
                         dest="ticketNumber",
                         help="set ticket number")
    parser.add_option("--name", action="store",
                         dest="customerName",
                         help="define customer name")
    parser.add_option("--config-file", action="store",
                         dest="config_file",
                         help="specify alternate configuration file")
    parser.add_option("--tmp-dir", action="store",
                         dest="tmp_dir",
                         help="specify alternate temporary directory", default=tempfile.gettempdir())
    parser.add_option("--diagnose", action="store_true",
                         dest="diagnose",
                         help="enable diagnostics", default=False)
    parser.add_option("--analyze", action="store_true",
                         dest="analyze",
                         help="enable analyzations", default=False)
    parser.add_option("--report", action="store_true",
                         dest="report",
                         help="Enable html/xml reporting", default=False)
    parser.add_option("--profile", action="store_true",
                         dest="profiler",
                         help="turn on profiling", default=False)
    parser.add_option("-z", "--compression-type", dest="compression_type",
                        help="compression technology to use [auto, zip, gzip, bzip2, xz] (default=auto)",
                        default="auto")

    return parser.parse_args(opts)


class XmlReport(object):
    """ Report build class """
    def __init__(self):
        try:
            import libxml2
        except ImportError:
            self.enabled = False
            return
        else:
            self.enabled = False
            return
        self.doc = libxml2.newDoc("1.0")
        self.root = self.doc.newChild(None, "sos", None)
        self.commands = self.root.newChild(None, "commands", None)
        self.files = self.root.newChild(None, "files", None)

    def add_command(self, cmdline, exitcode, stdout = None, stderr = None,
                    f_stdout=None, f_stderr=None, runtime=None):
        """ Appends command run into report """
        if not self.enabled:
            return

        cmd = self.commands.newChild(None, "cmd", None)

        cmd.setNsProp(None, "cmdline", cmdline)

        cmdchild = cmd.newChild(None, "exitcode", str(exitcode))

        if runtime:
            cmd.newChild(None, "runtime", str(runtime))

        if stdout or f_stdout:
            cmdchild = cmd.newChild(None, "stdout", stdout)
            if f_stdout:
                cmdchild.setNsProp(None, "file", f_stdout)

        if stderr or f_stderr:
            cmdchild = cmd.newChild(None, "stderr", stderr)
            if f_stderr:
                cmdchild.setNsProp(None, "file", f_stderr)

    def add_file(self, fname, stats):
        """ Appends file(s) added to report """
        if not self.enabled:
            return

        cfile = self.files.newChild(None, "file", None)

        cfile.setNsProp(None, "fname", fname)

        cchild = cfile.newChild(None, "uid", str(stats[ST_UID]))
        cchild = cfile.newChild(None, "gid", str(stats[ST_GID]))
        cfile.newChild(None, "mode", str(oct(S_IMODE(stats[ST_MODE]))))
        cchild = cfile.newChild(None, "ctime", strftime('%a %b %d %H:%M:%S %Y',
                                                        localtime(stats[ST_CTIME])))
        cchild.setNsProp(None, "tstamp", str(stats[ST_CTIME]))
        cchild = cfile.newChild(None, "atime", strftime('%a %b %d %H:%M:%S %Y',
                                                        localtime(stats[ST_ATIME])))
        cchild.setNsProp(None, "tstamp", str(stats[ST_ATIME]))
        cchild = cfile.newChild(None, "mtime", strftime('%a %b %d %H:%M:%S %Y',
                                                        localtime(stats[ST_MTIME])))
        cchild.setNsProp(None, "tstamp", str(stats[ST_MTIME]))

    def serialize(self):
        """ Serializes xml """
        if not self.enabled:
            return

        self.ui_log.info(self.doc.serialize(None,  1))

    def serialize_to_file(self, fname):
        """ Serializes to file """
        if not self.enabled:
            return

        outf = tempfile.NamedTemporaryFile()
        outf.write(self.doc.serialize(None, 1))
        outf.flush()
        self.archive.add_file(outf.name, dest=fname)
        outf.close()


class SoSReport(object):

    def __init__(self, opts):
        self.loaded_plugins = deque()
        self.skipped_plugins = deque()
        self.all_options = deque()
        self.xml_report = XmlReport()

        try:
            import signal
            signal.signal(signal.SIGTERM, self.get_exit_handler())
        except Exception:
            pass # not available in java, but we don't care


        self.opts, self.args = parse_options(opts)
        self.tempfile_util = TempFileUtil(tmp_dir=self.opts.tmp_dir)
        self._set_debug()
        self._read_config()
        self.policy = sos.policies.load()
        self._is_root = self.policy.is_root()
        self._set_directories()
        self._setup_logging()
        self.policy.setCommons(self.get_commons())
        self.print_header()
        self.load_plugins()
        self._set_tunables()
        self._check_for_unknown_plugins()
        self._set_plugin_options()

    def print_header(self):
        self.ui_log.info("\n%s\n" % _("sosreport (version %s)" % (__version__,)))

    def get_commons(self):
        return {
                'cmddir': self.cmddir,
                'logdir': self.logdir,
                'rptdir': self.rptdir,
                'soslog': self.soslog,
                'policy': self.policy,
                'verbosity': self.opts.verbosity,
                'xmlreport': self.xml_report,
                'cmdlineopts': self.opts,
                'config': self.config
                }

    def get_temp_file(self):
        return self.tempfile_util.new()

    def _set_archive(self):
        if self.opts.compression_type not in ('auto', 'zip', 'bzip2', 'gzip', 'xz'):
            raise Exception("Invalid compression type specified. Options are: auto, zip, bzip2, gzip and xz")
        archive_name = os.path.join(self.opts.tmp_dir,self.policy.getArchiveName())
        if self.opts.compression_type == 'auto':
            auto_archive = self.policy.preferedArchive()
            self.archive = auto_archive(archive_name)
        elif self.opts.compression_type == 'zip':
            self.archive = ZipFileArchive(archive_name)
        else:
            self.archive = TarFileArchive(archive_name)

    def _set_directories(self):
        self.cmddir = 'sos_commands'
        self.logdir = 'sos_logs'
        self.rptdir = 'sos_reports'

    def _set_debug(self):
        if self.opts.debug:
            sys.excepthook = self._exception
            self.raise_plugins = True
        else:
            self.raise_plugins = False

    @staticmethod
    def _exception(etype, eval_, etrace):
        """ Wrap exception in debugger if not in tty """
        if hasattr(sys, 'ps1') or not sys.stderr.isatty():
            # we are in interactive mode or we don't have a tty-like
            # device, so we call the default hook
            sys.__excepthook__(etype, eval_, etrace)
        else:
            import traceback, pdb
            # we are NOT in interactive mode, print the exception...
            traceback.print_exception(etype, eval_, etrace, limit=2, file=sys.stdout)
            print
            # ...then start the debugger in post-mortem mode.
            pdb.pm()

    def _exit(self, error=0):
        raise SystemExit()
#        sys.exit(error)

    def _exit_nice(self):
        for plugname, plugin in self.loaded_plugins:
            plugin.exit_please()
        self.ui_log.info("All processes ended, cleaning up.")
        self._exit(1)

    def get_exit_handler(self):
        def exit_handler(signum, frame):
            self._exit_nice()
        return exit_handler

    def _read_config(self):
        self.config = ConfigParser.ConfigParser()
        if self.opts.config_file:
            config_file = self.opts.config_file
        else:
            config_file = '/etc/sos.conf'
        try:
            self.config.readfp(open(config_file))
        except IOError:
            pass

    def _setup_logging(self):

        if not sys.stdin.isatty():
            self.opts.nocolors = True
            self.opts.batch = True

        # main soslog
        self.soslog = logging.getLogger('sos')
        self.soslog.setLevel(logging.DEBUG)
        self.sos_log_file = self.get_temp_file()
        self.sos_log_file.close()
        flog = logging.FileHandler(self.sos_log_file.name)
        flog.setFormatter(logging.Formatter('%(asctime)s %(levelname)s: %(message)s'))
        flog.setLevel(logging.INFO)
        self.soslog.addHandler(flog)

        if not self.opts.silent:
            console = logging.StreamHandler(sys.stderr)
            console.setFormatter(logging.Formatter('%(message)s'))
            if self.opts.verbosity > 1:
                console.setLevel(logging.DEBUG)
            elif self.opts.verbosity > 0:
                console.setLevel(logging.INFO)
            else:
                console.setLevel(logging.FATAL)
            self.soslog.addHandler(console)

        # ui log
        self.ui_log = logging.getLogger('sos_ui')
        self.ui_log.setLevel(logging.INFO)
        self.sos_ui_log_file = self.get_temp_file()
        self.sos_ui_log_file.close()
        ui_fhandler = logging.FileHandler(self.sos_ui_log_file.name)
        ui_fhandler.setFormatter(logging.Formatter('%(asctime)s %(levelname)s: %(message)s'))

        self.ui_log.addHandler(ui_fhandler)

        if not self.opts.silent:
            ui_console = logging.StreamHandler(sys.stdout)
            ui_console.setFormatter(logging.Formatter('%(message)s'))
            ui_console.setLevel(logging.INFO)
            self.ui_log.addHandler(ui_console)

        # profile logging
        if self.opts.profiler:
            self.proflog = logging.getLogger('sosprofile')
            self.proflog.setLevel(logging.DEBUG)
            self.sos_profile_log_file = self.get_temp_file()
            plog = logging.FileHandler(self.sos_profile_log_file.name)
            plog.setFormatter(logging.Formatter('%(message)s'))
            plog.setLevel(logging.DEBUG)
            self.proflog.addHandler(plog)

    def _finish_logging(self):
        logging.shutdown()

        # the logging module seems to persist in the jython/jboss/eap world
        # so the handlers need to be removed
        for logger in [logging.getLogger(x) for x in ('sos', 'sosprofile', 'sos_ui')]:
            for h in logger.handlers:
                logger.removeHandler(h)

        if getattr(self, "sos_log_file", None):
            self.archive.add_file(self.sos_log_file.name, dest=os.path.join('sos_logs', 'sos.log'))
        if getattr(self, "sos_profile_log_file", None):
            self.archive.add_file(self.sos_profile_log_file.name, dest=os.path.join('sos_logs', 'profile.log'))
        if getattr(self, "sos_ui_log_file", None):
            self.archive.add_file(self.sos_ui_log_file.name, dest=os.path.join('sos_logs', 'ui.log'))

    def _get_disabled_plugins(self):
        disabled = []
        if self.config.has_option("plugins", "disable"):
            disabled = [plugin.strip() for plugin in
                        self.config.get("plugins", "disable").split(',')]
        return disabled

    def _is_skipped(self, plugin_name):
        return (plugin_name in self.opts.noplugins or
                plugin_name in self._get_disabled_plugins())

    def _is_inactive(self, plugin_name, pluginClass):
        return (not pluginClass(self.get_commons()).checkenabled() and
                not plugin_name in self.opts.enableplugins  and
                not plugin_name in self.opts.onlyplugins)

    def _is_not_default(self, plugin_name, pluginClass):
        return (not pluginClass(self.get_commons()).defaultenabled() and
                not plugin_name in self.opts.enableplugins and
                not plugin_name in self.opts.onlyplugins)

    def _is_not_specified(self, plugin_name):
        return (self.opts.onlyplugins and
                not plugin_name in self.opts.onlyplugins)

    def _skip(self, plugin_class, reason="unknown"):
        self.skipped_plugins.append((
            plugin_class.name(),
            plugin_class(self.get_commons()),
            reason
        ))

    def _load(self, plugin_class):
        self.loaded_plugins.append((
            plugin_class.name(),
            plugin_class(self.get_commons())
        ))


    def load_plugins(self):

        helper = ImporterHelper(package_path=os.path.join('sos', 'plugins'))
        plugins = helper.get_modules()
        self.plugin_names = deque()

        # validate and load plugins
        for plug in plugins:
            plugbase, ext = os.path.splitext(plug)
            try:
                plugin_classes = import_plugin(plugbase)

                for plugin_class in plugin_classes:
                    if not self.policy.validatePlugin(plugin_class):
                        self.soslog.debug(_("plugin %s does not validate, skipping") % plug)
                        self._skip(plugin_class, _("does not validate"))
                        continue

                    if plugin_class.requires_root and not self._is_root:
                        self.soslog.debug(_("plugin %s requires root permissions to execute, skipping") % plug)
                        self._skip(plugin_class, _("requires root"))
                        continue

                    # plug-in is valid, let's decide whether run it or not
                    self.plugin_names.append(plugbase)

                    if any((self._is_skipped(plugbase),
                            self._is_inactive(plugbase, plugin_class),
                            self._is_not_default(plugbase, plugin_class),
                            self._is_not_specified(plugbase),
                            )):
                        self._skip(plugin_class, _("inactive"))
                        continue

                    self._load(plugin_class)
            except Exception, e:
                self.soslog.warning(_("plugin %s does not install, skipping: %s") % (plug, e))
                if self.raise_plugins:
                    raise

    def _set_all_options(self):
        if self.opts.usealloptions:
            for plugname, plug in self.loaded_plugins:
                for name, parms in zip(plug.optNames, plug.optParms):
                    if type(parms["enabled"])==bool:
                        parms["enabled"] = True

    def _set_tunables(self):
        if self.config.has_section("tunables"):
            if not self.opts.plugopts:
                self.opts.plugopts = deque()

            for opt, val in self.config.items("tunables"):
                if not opt.split('.')[0] in self.disabled:
                    self.opts.plugopts.append(opt + "=" + val)
        if self.opts.plugopts:
            opts = {}
            for opt in self.opts.plugopts:
                # split up "general.syslogsize=5"
                try:
                    opt, val = opt.split("=")
                except:
                    val = True
                else:
                    if val.lower() in ["off", "disable", "disabled", "false"]:
                        val = False
                    else:
                        # try to convert string "val" to int()
                        try:
                            val = int(val)
                        except:
                            pass

                # split up "general.syslogsize"
                try:
                    plug, opt = opt.split(".")
                except:
                    plug = opt
                    opt = True

                try:
                    opts[plug]
                except KeyError:
                    opts[plug] = deque()
                opts[plug].append( (opt, val) )

            for plugname, plug in self.loaded_plugins:
                if plugname in opts:
                    for opt, val in opts[plugname]:
                        if not plug.setOption(opt, val):
                            self.soslog.error('no such option "%s" for plugin '
                                         '(%s)' % (opt,plugname))
                            self._exit(1)
                    del opts[plugname]
            for plugname in opts.keys():
                self.soslog.error('unable to set option for disabled or non-existing '
                             'plugin (%s)' % (plugname))

    def _check_for_unknown_plugins(self):
        import itertools
        for plugin in itertools.chain(self.opts.onlyplugins,
                                      self.opts.noplugins,
                                      self.opts.enableplugins):
            plugin_name = plugin.split(".")[0]
            if not plugin_name in self.plugin_names:
                self.soslog.fatal('a non-existing plugin (%s) was specified in the '
                             'command line' % (plugin_name))
                self._exit(1)

    def _set_plugin_options(self):
        for plugin_name, plugin in self.loaded_plugins:
            names, parms = plugin.getAllOptions()
            for optname, optparm in zip(names, parms):
                self.all_options.append((plugin, plugin_name, optname, optparm))

    def list_plugins(self):
        if not self.loaded_plugins and not self.skipped_plugins:
            self.soslog.fatal(_("no valid plugins found"))
            self._exit(1)

        if self.loaded_plugins:
            self.ui_log.info(_("The following plugins are currently enabled:"))
            self.ui_log.info("")
            for (plugname, plug) in self.loaded_plugins:
                self.ui_log.info(" %-15s %s" % (plugname, plug.get_description()))
        else:
            self.ui_log.info(_("No plugin enabled."))
        self.ui_log.info("")

        if self.skipped_plugins:
            self.ui_log.info(_("The following plugins are currently disabled:"))
            self.ui_log.info("")
            for (plugname, plugclass, reason) in self.skipped_plugins:
                self.ui_log.info(" %-15s %-14s %s" % (plugname,
                                     reason,
                                     plugclass.get_description()))
        self.ui_log.info("")

        if self.all_options:
            self.ui_log.info(_("The following plugin options are available:"))
            self.ui_log.info("")
            for (plug, plugname, optname, optparm)  in self.all_options:
                # format and colorize option value based on its type (int or bool)
                if type(optparm["enabled"]) == bool:
                    if optparm["enabled"] == True:
                        tmpopt = "on"
                    else:
                        tmpopt = "off"
                else:
                    tmpopt = optparm["enabled"]

                self.ui_log.info(" %-25s %-15s %s" % (
                    plugname + "." + optname, tmpopt, optparm["desc"]))
        else:
            self.ui_log.info(_("No plugin options available."))

        self.ui_log.info("")
        self._exit()

    def batch(self):
        if self.opts.batch:
            self.ui_log.info(self.policy.get_msg())
        else:
            msg = self.policy.get_msg()
            msg += _("Press ENTER to continue, or CTRL-C to quit.\n")
            try:
                raw_input(msg)
            except:
                self.ui_log.info("")
                self._exit()

    def _log_plugin_exception(self, plugin_name):
        self.soslog.error("%s\n%s" % (plugin_name, traceback.format_exc()))

    def diagnose(self):
        tmpcount = 0
        for plugname, plug in self.loaded_plugins:
            try:
                plug.diagnose()
            except:
                if self.raise_plugins:
                    raise
                else:
                    self._log_plugin_exception(plugname)

            tmpcount += len(plug.diagnose_msgs)
        if tmpcount > 0:
            self.ui_log.info(_("One or more plugins have detected a problem in your "
                "configuration."))
            self.ui_log.info(_("Please review the following messages:"))
            self.ui_log.info("")

            fp = self.get_temp_file()
            for plugname, plug in self.loaded_plugins:
                for tmpcount2 in range(0, len(plug.diagnose_msgs)):
                    if tmpcount2 == 0:
                        soslog.warning("%s:" % plugname)
                    soslog.warning("    * %s" % plug.diagnose_msgs[tmpcount2])
                    fp.write("%s: %s\n" % (plugname, plug.diagnose_msgs[tmpcount2]))
            self.archive.add_file(fp.name, dest=os.path.join(self.rptdir, 'diagnose.txt'))

            self.ui_log.info("")
            if not self.opts.batch:
                try:
                    while True:
                        yorno = raw_input( _("Are you sure you would like to "
                                             "continue (y/n) ? ") )
                        if yorno == _("y") or yorno == _("Y"):
                            self.ui_log.info("")
                            break
                        elif yorno == _("n") or yorno == _("N"):
                            self._exit(0)
                    del yorno
                except KeyboardInterrupt:
                    self.ui_log.info("")
                    self._exit(0)

    def prework(self):
        try:
            self.policy.preWork()
            self._set_archive()
        except Exception, e:
            import traceback
            traceback.print_exc(e)
            self.ui_log.info(e)
            self._exit(0)

    def setup(self):
        for plugname, plug in self.loaded_plugins:
            try:
                plug.setArchive(self.archive)
                plug.setup()
            except KeyboardInterrupt:
                raise
            except:
                if self.raise_plugins:
                    raise
                else:
                    self._log_plugin_exception(plugname)

    def version(self):
        """Fetch version information from all plugins and store in the report
        version file"""

        versions = []
        versions.append("sosreport: %s" % __version__)
        for plugname, plug in self.loaded_plugins:
            versions.append("%s: %s" % (plugname, plug.version))
        self.archive.add_string(content="\n".join(versions), dest='version.txt')


    def copy_stuff(self):
        plugruncount = 0
        for i in izip(self.loaded_plugins):
            plugruncount += 1
            plugname, plug = i[0]
            if not self.opts.silent:
                sys.stdout.write("\r  Running %d/%d: %s...        " % (plugruncount, len(self.loaded_plugins), plugname))
                sys.stdout.flush()
            try:
                plug.copyStuff()
            except KeyboardInterrupt:
                raise
            except:
                if self.raise_plugins:
                    raise
                else:
                    self._log_plugin_exception(plugname)

    def report(self):
        for plugname, plug in self.loaded_plugins:
            for oneFile in plug.copiedFiles:
                try:
                    self.xml_report.add_file(oneFile["srcpath"], os.stat(oneFile["srcpath"]))
                except:
                    pass

        self.xml_report.serialize_to_file(
            os.path.join(self.rptdir, "sosreport.xml"))


    def plain_report(self):
        report = Report()

        for plugname, plug in self.loaded_plugins:
            section = Section(name=plugname)

            for alert in plug.alerts:
                section.add(Alert(alert))

            if plug.customText:
                section.add(Note(plug.customText))

            for f in plug.copiedFiles:
                section.add(CopiedFile(name=f["srcpath"], href=f["dstpath"]))

            for cmd in plug.executedCommands:
                section.add(Command(name=cmd['exe'], return_code=0, href=cmd['file']))

            for content, f in plug.copyStrings:
                section.add(CreatedFile(name=f))

            report.add(section)

        fd = self.get_temp_file()
        fd.write(str(PlainTextReport(report)))
        fd.flush()
        self.archive.add_file(fd.name, dest=os.path.join('sos_reports', 'sos.txt'))


    def html_report(self):
        # Generate the header for the html output file
        rfd = self.get_temp_file()
        rfd.write("""
        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
            <head>
        <link rel="stylesheet" type="text/css" media="screen" href="donot.css" />
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <title>Sos System Report</title>
            </head>

            <body>
        """)


        # Make a pass to gather Alerts and a list of module names
        allAlerts = deque()
        plugNames = deque()
        for plugname, plug in self.loaded_plugins:
            for alert in plug.alerts:
                allAlerts.append('<a href="#%s">%s</a>: %s' % (plugname, plugname,
                                                               alert))
            plugNames.append(plugname)

        # Create a table of links to the module info
        rfd.write("<hr/><h3>Loaded Plugins:</h3>")
        rfd.write("<table><tr>\n")
        rr = 0
        for i in range(len(plugNames)):
            rfd.write('<td><a href="#%s">%s</a></td>\n' % (plugNames[i],
                                                           plugNames[i]))
            rr = divmod(i, 4)[1]
            if (rr == 3):
                rfd.write('</tr>')
        if not (rr == 3):
            rfd.write('</tr>')
        rfd.write('</table>\n')

        rfd.write('<hr/><h3>Alerts:</h3>')
        rfd.write('<ul>')
        for alert in allAlerts:
            rfd.write('<li>%s</li>' % alert)
        rfd.write('</ul>')


        # Call the report method for each plugin
        for plugname, plug in self.loaded_plugins:
            try:
                html = plug.report()
            except:
                if self.raise_plugins:
                    raise
            else:
                rfd.write(html)

        rfd.write("</body></html>")

        rfd.flush()

        self.archive.add_file(rfd.name, dest=os.path.join('sos_reports', 'sos.html'))

    def postproc(self):
        for plugname, plug in self.loaded_plugins:
            try:
                plug.postproc()
            except:
                if self.raise_plugins:
                    raise

    def final_work(self):

        # package up the results for the support organization
        self.policy.packageResults(self.archive.name())

        self._finish_logging()

        self.archive.close()

        final_filename = compress(self.archive, self.opts.compression_type)

        # automated submission will go here
        if not self.opts.upload:
            self.policy.displayResults(final_filename)
        else:
            self.policy.uploadResults(final_filename)

        self.tempfile_util.clean()

        return final_filename

    def ensure_plugins(self):
        if not self.loaded_plugins:
            self.soslog.error(_("no valid plugins were enabled"))
            self._exit(1)

def main(args):
    """The main entry point"""
    try:
        sos = SoSReport(args)

        if sos.opts.listPlugins:
            sos.list_plugins()

        sos.ensure_plugins()
        sos.batch()

        if sos.opts.diagnose:
            sos.diagnose()

        sos.prework()
        sos.setup()

        sos.ui_log.info(_(" Running plugins. Please wait ..."))
        sos.ui_log.info("")

        sos.copy_stuff()

        sos.ui_log.info("")

        if sos.opts.report:
            sos.report()
            sos.html_report()
            sos.plain_report()

        sos.postproc()
        sos.version()

        return sos.final_work()
    except SystemExit:
        return None
