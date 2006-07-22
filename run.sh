#!/bin/sh

curdir="$(cd "$(dirname "$0")"; pwd)"
java -jar "$curdir"/../ImageJ/ij.jar -Dplugins.dir="$curdir" "$@"
