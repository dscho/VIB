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

MEM=256m
case $HOSTNAME in
wbgn129)
	MEM=1500m
esac

case "$1" in
app)
	shift
	java -Xmx256m -Dplugins.dir="$curdir" $EXTRADEFS \
		-classpath "$curdir"/../ImageJ/ij.jar$CPSEP. vib.app.App "$@"
	;;
*)
	java -Xmx$MEM -Dplugins.dir="$curdir" $EXTRADEFS \
		-jar "$curdir"/../ImageJ/ij.jar "$@"
	;;
esac

