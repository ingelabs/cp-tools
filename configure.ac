dnl Process this file with automake to produce configure.

AC_INIT(cp-tools, 0.00-cvs, [cp-tools-discuss@gnu.org])
AM_INIT_AUTOMAKE(tar-ustar)

AC_DISABLE_STATIC

AM_PROG_GCJ
AC_PROG_LIBTOOL
AC_PROG_JAR
AC_CHECK_CLASSPATH
AC_PROG_JAVA

dnl Check if required jars are in $prefix/share/java and prepend it to the
dnl CLASSPATH.

if test "x$prefix" = "xNONE"
then
  export jar_prefix=/usr/local
else
  export jar_prefix=$prefix
fi

AC_CHECK_FILE([$jar_prefix/share/java/kawa.jar],
  if test -z $CLASSPATH
  then
    export CLASSPATH=$jar_prefix/share/java/kawa.jar:.
  else
    export CLASSPATH=$jar_prefix/share/java/kawa.jar:$CLASSPATH:.
  fi
  KAWA_JAR=$jar_prefix/share/java/kawa.jar
)

if test -d /usr/share/classpath
then
  classpath_prefix=/usr
elif test -d /usr/classpath/share/classpath
then
  classpath_prefix=/usr/classpath
elif test -d /usr/local/share/classpath
then
  classpath_prefix=/usr/local
elif test -d /usr/local/classpath/share/classpath
then
  classpath_prefix=/usr/local/classpath
fi

AC_CHECK_FILE([$classpath_prefix/share/classpath/glibj.zip],
  if test -z $CLASSPATH
  then
    export CLASSPATH=$classpath_prefix/share/classpath/glibj.zip:.
  else
    export CLASSPATH=$classpath_prefix/share/classpath/glibj.zip:$CLASSPATH:.
  fi
)

AC_SUBST(CLASSPATH)
AC_SUBST(KAWA_JAR)

AC_CHECK_RQRD_CLASS([gnu.bytecode.ClassFileInput])
AC_CHECK_RQRD_CLASS([javax.xml.transform.Transformer])

AC_OUTPUT([Makefile])
