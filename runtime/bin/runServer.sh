#!/bin/sh

LIBS="../build/J9P.core-1.1.jar:../build/J9P.engines-1.1.jar"
for JAR in `find lib -name *.jar`; do
	LIBS="${LIBS}:${JAR}"
done

java -cp ${LIBS} j9p.StyxServer $*
