#!/bin/sh

curdir="$(cd "$(dirname "$0")"; pwd)"
case `uname -o 2>/dev/null`
in Cygwin*) curdir=$(cygpath --mixed $curdir);;
esac

java -Dplugins.dir="$curdir" -jar "$curdir"/../ImageJ/ij.jar "$@"
