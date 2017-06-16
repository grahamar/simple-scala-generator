#!/bin/bash

# Build the generator package, run then named generator (client|server), and
# attempt to build the generated sources.

SBT=sbt
CODEGEN=swagger-codegen/swagger-codegen

GEN_ARG=$1
GEN_NAME=$1
SWAGGER_SPEC=$2
GEN_NAME=simple-scala

if [ -z $SWAGGER_SPEC ]; then
  SWAGGER_SPEC=swagger.yaml
fi

echo "[ $GEN_NAME generator test ]"

# Use SBT to build the package and print the target directory, name, and version.
# Disable log formatting and grab the last three lines, which will consist of the above
# values, respectively.
echo "  -> Building the generator package ..."
parts=`SBT_OPTS='-Dsbt.log.noformat=true' $SBT 'package' 'show target' 'show name' 'show version' | tail -n 3 | cut -d ' ' -f 2`

# Push those lines into variables so we can construct a full path to the artifact.
for i in $parts; do
  if [ -z $A_DIR ]; then
    A_DIR=$i
  elif [ -z $A_NAME ]; then
    A_NAME=$i
  else
    A_VERSION=$i
  fi
done

# The complete artifact path.
A_PATH=$A_DIR/scala-2.11/${A_NAME}_2.11-$A_VERSION.jar

GEN_DIR=generated-$GEN_ARG
BUILD_OUT=$TMPDIR/generated-build-$RANDOM.out

echo "  -> Running the code generator ..."
A_CP=`SBT_OPTS='-Dsbt.log.noformat=true' $SBT 'export runtime:fullClasspath' | tail -n 1 | cut -d ' ' -f 2`
echo "CP=$A_CP:$A_PATH $CODEGEN generate -i $SWAGGER_SPEC -l $GEN_NAME -o $GEN_DIR -codec play > $BUILD_OUT 2>&1"
CP=$A_CP:$A_PATH $CODEGEN generate -i $SWAGGER_SPEC -l $GEN_NAME -o $GEN_DIR > $BUILD_OUT 2>&1
RES=$?

# It's possible to fail here, most interestingly for malformed swagger.
# Exit with code of the code generation attempt.
if [ $RES -ne 0 ]; then
  cat $BUILD_OUT
  rm $BUILD_OUT
  echo "[ FAILED with exit code $RES ]"
  exit $RES
else
  echo "[ PASSED ]"
fi

# Currently client generation does not produce a project, as its intended to output source
# within an existing code base. Thus we provide one here so we can test the generated code
# via compilation.

echo 'scalaVersion := "2.11.8"

libraryDependencies ++= Seq("com.typesafe.play" %% "play-json" % "2.5.15")
' > $GEN_DIR/build.sbt

echo "  -> Attempting to build the generated code ..."
pushd $GEN_DIR > /dev/null
BUILD_OUT=$TMPDIR/generated-build-$RANDOM.out
$SBT compile > $BUILD_OUT
RES=$?
popd > /dev/null

# Exit with code of the build attempt.
if [ $RES -ne 0 ]; then
  cat $BUILD_OUT
  echo "[ FAILED with exit code $RES ]"
else
  echo "[ PASSED ]"
fi

rm $BUILD_OUT

exit $RES
