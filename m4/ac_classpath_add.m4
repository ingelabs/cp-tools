dnl -------------------------------------------------------------
dnl Macro to add argument to CLASSPATH
dnl -------------------------------------------------------------
AC_DEFUN([AC_CLASSPATH_ADD],[
  classpath_repeat=false
  test -z $CLASSPATH || \
    OLD_IFS="$IFS"; \
    IFS=":"; \
    for i in $CLASSPATH; do \
      if test "x$i" = "x$1"; then \
        classpath_repeat=true; \
      fi; \
    done; \
    IFS="$OLD_IFS"

  if test -z $CLASSPATH && test "x$classpath_repeat" != "xtrue"
  then
    CLASSPATH=$1
  else
    CLASSPATH=$CLASSPATH:$1
  fi
  export CLASSPATH
])

