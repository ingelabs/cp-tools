dnl Process this file with automake to produce configure.

AC_INIT(cp-tools, 0.00-cvs, [cp-tools-discuss@gnu.org])
AM_INIT_AUTOMAKE

AC_DISABLE_STATIC

AM_PROG_GCJ
AC_PROG_LIBTOOL
AC_PROG_JAR
AC_CHECK_CLASSPATH
AC_PROG_JAVA
AC_PROG_JAVAC

dnl Check if required jars are in $prefix/share/java and prepend it to the
dnl CLASSPATH.


if test -z $CLASSPATH
then
  export CLASSPATH=.
else
  export CLASSPATH=$CLASSPATH:.
fi

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

dnl
dnl Compile natively with gcj?
dnl
AC_ARG_ENABLE([native],
  [AS_HELP_STRING(--disable-native, disable generation of native binaries [[default=no]])],
  [
    if test "x${withval}" != xno; then
      enable_gcj=no
    else
      enable_gcj=yes
    fi
  ],
  [
    enable_gcj=yes
  ]
)
if test "x${GCJ}" = xfalse; then
  enable_gcj=no
  echo "Native compilation disabled because gcj not found"
fi
AM_CONDITIONAL(ENABLE_GCJ_BINARY, test "x${enable_gcj}" = xyes)

dnl
dnl GNU bytecode library
dnl
AC_ARG_WITH([gnu-bytecode],
  [AS_HELP_STRING(--with-gnu-bytecode,use gnu.bytecode [[default=yes]])],
  [
    if test "x${withval}" != xno; then
      with_gnubytecode=yes
    else
      with_gnubytecode=no
    fi
  ],
  [
    with_gnubytecode=yes
  ]
)
AM_CONDITIONAL(USE_GNUBYTECODE, test "x${with_gnubytecode}" = xyes)
if test "x${with_gnubytecode}" = xyes
then 
  for path in /usr /usr/local; do
    AC_CHECK_FILE([$path/share/java/bytecode.jar],
      AC_CLASSPATH_ADD([$path/share/java/bytecode.jar])
      BYTECODE_JAR=$path/share/java/bytecode.jar
      export BYTECODE_JAR
    )
  done
  test -d src || mkdir src
  test -d src/jars || mkdir src/jars
  cp -f ${BYTECODE_JAR} src/jars/
fi

dnl
dnl ASM bytecode library
dnl
AC_ARG_WITH([asm],
  [AS_HELP_STRING(--with-asm,which asm jar to use)],
  [ASM_JAR=${withval}]
)
if test "x${ASM_JAR}" = x
then
  for path in /usr /usr/local; do
    AC_CHECK_FILE([$path/share/java/asm.jar],
      ASM_JAR=$path/share/java/asm.jar
    )
  done
fi
AC_CHECK_FILE([$ASM_JAR],
  [
    AC_CLASSPATH_ADD([$ASM_JAR])
    test -d src || mkdir src
    test -d src/jars || mkdir src/jars
    cp -f ${ASM_JAR} src/jars/asm.jar
  ],
  [AC_MSG_ERROR([ASM jar not found, exiting.])]
)

AC_SUBST(CLASSPATH)

if test "x${with_gnubytecode}" = xyes
then 
AC_CHECK_RQRD_CLASS([gnu.bytecode.ClassFileInput])
fi
AC_CHECK_RQRD_CLASS([javax.xml.transform.Transformer])

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
