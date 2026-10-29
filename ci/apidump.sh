#!/usr/bin/env bash
# Dumps class signatures (javap) of the remapped Minecraft jar so code can be
# written against the exact 1.21.11 API. Output: $1/api.txt and $1/classes.txt
set -u
OUT="$1"
JAR=$(find .gradle/loom-cache ~/.gradle/caches/fabric-loom -name 'minecraft-merged*.jar' ! -name '*sources*' 2>/dev/null | head -1)
[ -z "$JAR" ] && { echo "no minecraft jar found" > "$OUT/api.txt"; exit 0; }
unzip -Z1 "$JAR" | grep '\.class$' | sed 's/\.class$//; s#/#.#g' | sort > "$OUT/classes.txt"
: > "$OUT/api.txt"
while read -r line; do
  [ -z "$line" ] && continue
  if [[ "$line" == grep:* ]]; then
    pat="${line#grep:}"
    names=$(grep -E "\.${pat}(\$|\\$)" "$OUT/classes.txt" | grep -v '\$[0-9]')
  else
    names="$line"
  fi
  for n in $names; do
    echo "===== $n" >> "$OUT/api.txt"
    javap -p -cp "$JAR" "$n" >> "$OUT/api.txt" 2>&1
  done
done < ci/api-classes.txt
