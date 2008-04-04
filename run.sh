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
case `hostname` in
VIB|Leonhard)
	MEM=1000m
	;;
wbgn129)
	MEM=1500m
	;;
racer)
	MEM=256m
	;;
rudi)
	MEM=1000m
	;;
rioja|cognac.brookfield.local)
	MEM=1024m
	;;
esac

# Profiling stuff (see http://jiprof.sourceforge.net/)

test -f "$curdir"/profile.jar &&
	EXTRADEFS="$EXTRADEFS -javaagent:$curdir/profile.jar"
test -f "$curdir"/profile.properties &&
	EXTRADEFS="$EXTRADEFS -Dprofile.properties:$curdir/profile.properties"

case "$DEBUGIJ" in
''|echo)
	$DEBUGIJ java -Xmx$MEM -Dplugins.dir="$curdir" $EXTRADEFS \
		-jar "$curdir"/../ImageJ/ij.jar "$@"
;;
*)
	jdb -classpath "$curdir"/../ImageJ/ij.jar ij.ImageJ "$@"
esac

