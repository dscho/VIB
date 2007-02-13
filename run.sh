#!/bin/sh

curdir="$(cd "$(dirname "$0")"; pwd)"
case `uname -o 2>/dev/null` in
Cygwin*)
	curdir=$(cygpath --mixed $curdir)
	CPSEP=\;
	;;
*)
	CPSEP=:
	;;
esac

case $(hostname) in
racer)
	EXTRADEFS="$EXTRADEFS -Dj3d.noOffScreen=true"
esac

case "$1" in
app)
	shift
	java -Xmx256m -Dplugins.dir="$curdir" $EXTRADEFS \
		-classpath "$curdir"/../ImageJ/ij.jar$CPSEP. vib.app.App "$@"
	;;
*)
	java -Xmx256m -Dplugins.dir="$curdir" $EXTRADEFS \
		-jar "$curdir"/../ImageJ/ij.jar "$@"
	;;
esac

