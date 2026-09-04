# Increase the maximum file descriptors if we can.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      /*)
        MAX_FD_LIMIT=$(( $( stat -f%z /proc/self/fd ) - 3 ))
        ;;
      *)
        MAX_FD_LIMIT=$MAX_FD
        ;;
    esac
    ulimit -n "$MAX_FD_LIMIT" 2>/dev/null || true
fi

# Collect all arguments for the java command, stacking in reverse order:
#   * args from the command line
#   * the main class name
#   * -classpath
#   * -D...sysproperties
#   * --module-path
#   * the jvm arguments

# For Cygwin or MSYS, switch paths to Windows format before running java:
if "$cygwin" || "$msys" ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )
    CLASSPATH=$( cygpath --path --mixed "$CLASSPATH" )
