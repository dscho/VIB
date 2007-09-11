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
VIB)
	MEM=1000m
	;;
wbgn129)
	MEM=1500m
	;;
racer)
	MEM=320m
	#EXTRADEFS="$EXTRADEFS -Dj3d.noOffScreen=true"
	;;
rudi)
	MEM=1000m
	;;
esac

case "$DEBUGIJ" in
'')
	java -Xmx$MEM -Dplugins.dir="$curdir" $EXTRADEFS \
		-jar "$curdir"/../ImageJ/ij.jar "$@"
;;
*)
	jdb -classpath "$curdir"/../ImageJ/ij.jar ij.ImageJ "$@"
esac

