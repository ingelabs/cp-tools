dnl Available from the GNU Autoconf Macro Archive at:
dnl http://www.gnu.org/software/ac-archive/htmldoc/ac_prog_javac.html
dnl
AC_DEFUN([AC_PROG_JAVAC],[
AC_REQUIRE([AC_EXEEXT])dnl
ECJ_OPTS="-1.5 -warn:-deprecation,serial,unusedImport,unchecked,raw"
JAVAC_OPTS="-Xlint:cast,divzero,empty,finally,overrides"
if test "x$JAVAPREFIX" = x; then
        test "x$JAVAC" = x && AC_CHECK_PROGS(JAVAC, jikes$EXEEXT ["ecj$EXEEXT $ECJ_OPTS"] ["ecj-3.3$EXEEXT $ECJ_OPTS"] ["ecj-3.2$EXEEXT $ECJ_OPTS"] ["javac$EXEEXT $JAVAC_OPTS"] "gcj$EXEEXT -C")
else
        test "x$JAVAC" = x && AC_CHECK_PROGS(JAVAC, jikes$EXEEXT ["ecj$EXEEXT $ECJ_OPTS"] ["ecj-3.3$EXEEXT $ECJ_OPTS"] ["ecj-3.2$EXEEXT $ECJ_OPTS"] ["javac$EXEEXT $JAVAC_OPTS"] "gcj$EXEEXT -C", $JAVAPREFIX)
fi
test "x$JAVAC" = x && AC_MSG_ERROR([no acceptable Java compiler found in \$PATH])
AC_PROG_JAVAC_WORKS
if (echo "$JAVAC" | grep -e "gcj" >/dev/null 2>/dev/null); then
  case $(gcj -dumpversion) in
    4.3*)
      ;;
    *)
      AC_MSG_WARN([ The build seems to be using a version of gcj prior
to 4.3 for bytecode generation.  Some versions of gcj are known to
produce bad bytecode.  See here for a list of bugs that may be
relevant:

http://gcc.gnu.org/bugzilla/buglist.cgi?component=java&keywords=wrong-code&order=default

You may want to set the environment variable JAVAC to an alternate
compiler, such as jikes, to make sure that you end up with valid
bytecode.
]);
      ;;
  esac
fi
AC_PROVIDE([$0])dnl
])
