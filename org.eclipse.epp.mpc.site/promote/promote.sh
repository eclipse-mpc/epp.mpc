#!/bin/sh -e

DIST=$1
VERSION=$2
QUALIFIER=$3
SRC=$4
ARCHIVE=$5
SITE=$6

DST=$ARCHIVE/$VERSION/$QUALIFIER


if [ -e $DST ]; then
 echo $DST already exists
 exit 1
fi

echo Promoting $VERSION.$QUALIFIER to $DST

mkdir -p $DST/
unzip -d $DST/ $SRC
cp $SRC $DST/$DIST-$VERSION.$QUALIFIER.zip

chmod g+w -R $DST

cd $(dirname $0)
BASE=$(pwd)

if [ -n "$SITE" ]; then
 for i in $SITE $SITE/*; do
  if [ -e $i/composite.index ]; then
   echo "Updating $i"
   cd $i
   $BASE/update-composite.sh
  fi
 done
fi
