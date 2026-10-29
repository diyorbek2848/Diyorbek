#!/bin/bash
# Usage: probe.sh <decompiled source dir>
# Prints declarations (not bodies) of each class in classes.txt,
# plus any extra grep patterns in greps.txt (one match line each, max 5).
SRC="$1"
DIR="$(dirname "$0")"
while read -r cls; do
  [ -z "$cls" ] && continue
  f="$SRC/$(echo "$cls" | tr . /).java"
  echo "===== $cls"
  if [ ! -f "$f" ]; then echo "MISSING"; continue; fi
  grep -nE '^\s*(public|protected|private|static|final|abstract|record|enum|interface|class|@Nullable)[^=;]*(\(|\{|;)\s*$|^\s*(public|protected|private)?\s*(static )?(final )?[A-Za-z<>\[\]., ?]+ [a-zA-Z_]+\(.*\)\s*(throws [A-Za-z., ]+)?\s*\{?\s*$|^(public |final |abstract )*(class|record|interface|enum) ' "$f" | grep -vE '^\s*[0-9]+:\s*(return|if|for|while|switch|else|try|catch|throw|new |this\.|super\.)' | head -120
done < "$DIR/classes.txt"
echo "===== GREPS"
while read -r pat; do
  [ -z "$pat" ] && continue
  echo "--- $pat"
  grep -rnE "$pat" "$SRC/net/minecraft" "$SRC/com/mojang" 2>/dev/null | sed "s|$SRC/||" | head -6
done < "$DIR/greps.txt"
echo "===== FONTS"
find "$SRC" -path '*font*' -name '*.json' | head -20
