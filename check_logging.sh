#!/bin/bash

DIFF=$(git diff origin/main...HEAD)

PATTERNS=(
  ".info("
  ".infof("
  ".warn("
  ".warnf("
  ".error("
  ".errorf("
  ".fatal("
  ".fatalf("
  "System.out.print"
  "System.err.print"
  ".printStackTrace"
)

ERROR_FOUND=0
while IFS= read -r line; do
  # Check if the line is an added line
  if [[ "$line" =~ ^\+[^+] ]]; then
    # Ignore test directories
    if [[ "$line" != *"src/test/"* ]] && [[ "$line" != *"testsuite/"* ]]; then
      # Ignore comments
      if [[ "$line" != *"//"* ]]; then
        # Check for any of the patterns
        for pattern in "${PATTERNS[@]}"; do
          if [[ "$line" == *"$pattern"* ]]; then
            ERROR_FOUND=1
            break 2
          fi
        done
      fi
    fi
  fi
done <<< "$DIFF"

if [ "$ERROR_FOUND" -eq 1 ]; then
  echo "Logging statements found that should be internationalized or converted to a lower log level."
  exit 1
else
  echo "No problematic logging statements found."
  exit 0
fi
