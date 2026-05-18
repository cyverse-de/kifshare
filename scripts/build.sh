#!/bin/sh
set -eu

rm -rf resources
mkdir -p resources/css resources/js resources/img resources/fa

# JS: copy all top-level files from ui/src/js (kif.js, json2.js, etc.)
cp ui/src/js/*.js resources/js/

# CSS: compile kif.less, then copy sibling .css files and the images/ subdir
npx lessc ui/src/css/kif.less resources/css/kif.css
for f in ui/src/css/*.css; do
  [ -e "$f" ] && cp "$f" resources/css/
done
if [ -d ui/src/css/images ]; then
  cp -r ui/src/css/images resources/css/
fi

# Images
cp -r ui/src/img/. resources/img/

# Top-level static files
cp ui/src/robots.txt resources/
cp ui/ui.xml resources/

# Font Awesome (css + fonts)
cp -r ui/fa/. resources/fa/
