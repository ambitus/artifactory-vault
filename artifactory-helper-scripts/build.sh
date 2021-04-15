#!/bin/bash

# Build Variables
BUILD_DIR=$PWD
GIT_COMMIT=$(git rev-parse HEAD)
COMMIT_SHORT=${GIT_COMMIT:0:7}
COMMIT_SHORT=${COMMIT_SHORT^^}
HLQ="<HLQ>.C$COMMIT_SHORT"
export ARTIFACTORY_DOWNLOAD_HLQ="<HLQ>.C$COMMIT_SHORT"
export ARTIFACTORY_DOWNLOAD_HLQ=${ARTIFACTORY_DOWNLOAD_HLQ^^}

# Artifactory Variables
ARTIFACTORY_DIR=$BUILD_DIR/artifactory
ARTIFACT_NAME="BCPXMP_$COMMIT_SHORT"
export ARTIFACTORY_URL="http://<host or ip of artifactory server>:<port if necessary>/artifactory/"
# export ARTIFACTORY_TOEKN="Set this in manually. Access tokens should not be exposed in code."
export ARTIFACTORY_REPO="<artifactory repository>"
export ARTIFACTORY_UPLOAD="$ARTIFACTORY_DIR/artifacts-to-upload/$ARTIFACT_NAME.tar.gz"
export ARTIFACTORY_UPLOAD_TARGET="$ARTIFACT_NAME.tar.gz"
export ARTIFACTORY_DOWNLOAD_TARGET="$ARTIFACTORY_DIR/downloaded-artifacts/$ARTIFACT_NAME.tar.gz"
export ARTIFACTORY_REMOTE_ARTIFACT="$ARTIFACT_NAME.tar.gz"

# Build
cd ..
groovyz -D DBB_SUBPROCESS_ENABLED=true $BUILD_DIR/Build.groovy --loglvl ALL --hlq $HLQ $BUILD_DIR/build_all.txt \

# Prepare Artifactory uploads.
mkdir $ARTIFACTORY_DIR/artifacts-to-upload && mkdir $ARTIFACTORY_DIR/artifacts-to-upload/OBJ
cd $ARTIFACTORY_DIR/artifacts-to-upload
cp -AB "//'${HLQ}.OBJ'" OBJ
tar -zcvf $ARTIFACT_NAME.tar.gz OBJ

# Upload Artifact(s) to Artifactory
cd $ARTIFACTORY_DIR && groovyz UploadArtifact.groovy

# Download Artifact(s) from Artifactory
mkdir $ARTIFACTORY_DIR/downloaded-artifacts
groovyz DownloadArtifact.groovy

# Move Artifact(s) to MVS
cd $ARTIFACTORY_DIR/downloaded-artifacts
pax -rzvf $ARTIFACT_NAME.tar.gz
tso "allocate dsname('$ARTIFACTORY_DOWNLOAD_HLQ.OBJ') lrecl(80) recfm(f,b) dsorg(po) dsntype(library) blksize(32720)  space(16,5) cylinders"
cp -AB $ARTIFACTORY_DIR/downloaded-artifacts/OBJ/* "//'$ARTIFACTORY_DOWNLOAD_HLQ.OBJ'"

# Package Retrieved Artifacts with zService
cd ../../../properties/zService
./zpkg_genRelfiles.sh C$COMMIT_SHORT
