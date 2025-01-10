#!/bin/bash

BASE_BRANCH="${GITHUB_BASE_REF:-main}"
DIFF=$(git diff origin/"$BASE_BRANCH"...HEAD || true)

if [ -z "$DIFF" ]; then
  echo "No diff found to analyze."
  exit 0
fi

PATTERNS=(
  ".info(\""
  ".infof(\""
  ".warn(\""
  ".warnf(\""
  ".error(\""
  ".errorf(\""
  ".fatal(\""
  ".fatalf(\""
  "System.out.print"
  "System.err.print"
  ".printStackTrace"
)

ERRORS=""
CURRENT_FILE=""

while IFS= read -r line; do
  # Update the current file if a new diff starts
  if [[ "$line" =~ ^diff\ --git\ a\/.*\ b\/.* ]]; then
    CURRENT_FILE=$(echo "$line" | awk '{print $3}' | sed 's/^a\///')
  fi

  if [[ "$line" =~ ^\+[^+] ]]; then
    # Ignore lines in the test directories
    if [[ "$CURRENT_FILE" != *"src/test/"* && "$CURRENT_FILE" != *"testsuite/"* && "$CURRENT_FILE" != *"check_logging.sh"* ]]; then
      # Check for any of the patterns, ensuring "//" doesn't precede them
      for pattern in "${PATTERNS[@]}"; do
        if [[ "$line" == *"$pattern"* ]]; then
          # Ensure the pattern is not commented out with "//"
          if [[ "$line" != *"//"*"$pattern"* ]]; then
            # Capture the error line and its context
            ERRORS+="$line\nFile: $CURRENT_FILE\n\n"
          fi
        fi
      done
    fi
  fi
done <<< "$DIFF"

if [ -n "$ERRORS" ]; then
  echo -e "Logging statements found that should be internationalized or converted to a lower log level:\n"
  echo -e "$ERRORS"
  exit 1
else
  echo "No problematic logging statements found."
  exit 0
fi
