#!/usr/bin/env bash
set -e
set -x

PUBLISH_BASE=/home/data/httpd/download.eclipse.org/mpc/httpclient
PROMOTE_BASE=$PUBLISH_BASE/.promote
SITE_ID=org.eclipse.epp.mpc.apache.httpclient.site
DIST_NAME=mpc.apache.httpclient
TARGET_SITE=artifact/httpclient-target-site

do_promote() {
  DIST=$1
  VERSION=$2
  QUALIFIER=$3
  SRC=$4
  ARCHIVE=$5
  SITE=$6

  DST=$ARCHIVE/$VERSION/$QUALIFIER

  if [ -e "$DST" ]; then
    echo "$DST" already exists
    exit 1
  fi

  echo Promoting "$VERSION"."$QUALIFIER" to "$DST"

  umask 0002
  mkdir -p "$DST"/
  unzip -d "$DST"/ "$SRC"
  cp "$SRC" "$DST"/"$DIST"-"$VERSION"."$QUALIFIER".zip

  chmod g+rwX -R "$DST"
  chmod g+rwx "$ARCHIVE" "$ARCHIVE"/"$VERSION" || true

  cd $(dirname "$0")
  BASE=$(pwd)

  if [ -n "$SITE" ]; then
    for i in "$SITE" "$SITE"/*; do
      if [ -e "$i"/composite.index ]; then
        echo "Updating $i"
        cd "$i"
        update_composite
      fi
    done
  fi
}

compose() {
  FILE="$1"
  shift
  TAG="$1"
  shift
  TYPE="$1"
  shift
  NAME="$1"
  shift
  TIMESTAMP="$1"
  shift

  cat >"$FILE" <<-EOF
		<?xml version='1.0' encoding='UTF-8'?>
		<?$TAG version='1.0.0'?>
		<repository name='$NAME' type='$TYPE' version='1.0.0'>
		  <properties size='2'>
		    <property name='p2.compressed' value='true'/>
		    <property name='p2.timestamp' value='$TIMESTAMP'/>
		  </properties>
		  <children size='CHILD_COUNT'>
	EOF
  COUNT=0
  for i in "$@"; do
    echo "    <child location='$i'/>" >>"$FILE"
    COUNT=$((COUNT + 1))
  done
  sed -i -e "s/CHILD_COUNT/$COUNT/" "$FILE"

  cat >>"$FILE" <<-EOF
		  </children>
		</repository>
	EOF

  echo "Wrote $COUNT entries to $FILE"
}

update_composite() {
  source composite.index

  if [ "$DIRS" == "" ]; then
    echo "missing DIRS"
    exit 1
  fi

  if [ "$NAME" == "" ]; then
    echo "missing NAME"
    exit 1
  fi

  TIMESTAMP=$(date +%s)000

  compose compositeArtifacts.xml \
    compositeArtifactRepository \
    org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository \
    "$NAME" \
    "$TIMESTAMP" \
    $DIRS

  compose compositeContent.xml \
    compositeMetadataRepository \
    org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository \
    "$NAME" \
    "$TIMESTAMP" \
    $DIRS

}

download_build_archive() {
  ARCHIVE_PATH="$1"
  TARGET_PATH="$2"
  case "$TARGET_PATH" in
  */)
    TARGET_PATH="${TARGET_PATH}$(basename "$ARCHIVE_PATH")"
    ;;
  esac
  test -s "$TARGET_PATH" && echo "$TARGET_PATH already exists" && return 0
  curl -o "$TARGET_PATH" "${PROMOTED_URL}/$TARGET_SITE/${ARCHIVE_PATH}"
}

download_promote_package() {
  download_build_archive "$SITE_ID"/target/"$SITE_ID"-promote.zip ./promote.zip
  unzip ./promote.zip promote.properties
  . ./promote.properties
}

download_promoted_build() {
  download_promote_package

  SITE_ARCHIVE="$SITE_ID"-"$version"-SNAPSHOT.zip
  download_build_archive "$SITE_ID"/target/"$SITE_ARCHIVE" ./"$SITE_ARCHIVE"
}

promote_remote() {
  STAGE="$1"
  shift
  SITE_ARCHIVE="$1"
  shift

  case "$STAGE" in
  integration | release | train_release)
    echo "Promoting to $STAGE"
    ;;
  *)
    echo >&2 "Invalid stage $STAGE"
    return 1
    ;;
  esac

  REMOTE_WORKDIR=$(ssh genie.mpc@projects-storage.eclipse.org \
    "mkdir -p $PROMOTE_BASE && mktemp -p $PROMOTE_BASE -d $version-XXXXXXXX")
  scp promote.zip "$SITE_ARCHIVE" genie.mpc@projects-storage.eclipse.org:"$REMOTE_WORKDIR/"
  RESULT=0
  ssh genie.mpc@projects-storage.eclipse.org "cd $REMOTE_WORKDIR && unzip promote.zip && . promote.sh && promote_${STAGE}_remote" "$@" || RESULT=$?
  ssh genie.mpc@projects-storage.eclipse.org "rm -rf $PROMOTE_BASE"
  return $RESULT
}

promote_integration() {
  #TODO old nightlies should be deleted

  download_promoted_build
  . ./promote.properties
  SITE_ARCHIVE="$SITE_ID"-$version-SNAPSHOT.zip

  promote_remote integration "$SITE_ARCHIVE"
}

promote_integration_remote() {
  . ./promote.properties
  do_promote "$DIST_NAME" "$version" "$qualifier" \
    ./"$SITE_ID"-"$version"-SNAPSHOT.zip \
    $PUBLISH_BASE/drops \
    $PUBLISH_BASE/nightly

  # Update nightly composite
  cd $PUBLISH_BASE/nightly/latest
  update_composite
}

promote_release() {
  #TODO each x.x.x version release site should be a composite, too.

  download_promoted_build
  . ./promote.properties

  SITE_ARCHIVE="$SITE_ID"-$version-SNAPSHOT.zip
  test -z "$RELEASE" && RELEASE=$version
  promote_remote release "$SITE_ARCHIVE" "$RELEASE"
}

promote_release_remote() {
  RELEASE="$1"

  . ./promote.properties
  test -z "$RELEASE" && RELEASE=$version

  SOURCE="$SITE_ID"-$version-SNAPSHOT.zip
  TARGET=$PUBLISH_BASE/releases/$RELEASE

  rm -rf "$TARGET"
  mkdir -p "$TARGET"

  cp "$SOURCE" "$TARGET"/"$DIST_NAME"-"${version}".zip
  unzip -d "$TARGET"/ "$SOURCE"

  # Update latest release composite
  cd $PUBLISH_BASE/releases/latest
  update_composite
}

promote_train_release() {

  download_promote_package
  . ./promote.properties

  promote_remote train_release "" "$TRAIN"
}

promote_train_release_remote() {
  TRAIN="$1"

  IFS=' ,;' read -ra TRAINDIRS < <(echo "$TRAIN" | tr '[:upper:]' '[:lower:]')
  for t in "${TRAINDIRS[@]}"; do
    test ! -d $PUBLISH_BASE/"$t" && echo "Train $t does not exist" && continue
    cd $PUBLISH_BASE/"$t"
    update_composite
    test -d latest && cd latest && update_composite
  done
}
