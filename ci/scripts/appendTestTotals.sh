#!/bin/bash
#
# COPYRIGHT Ericsson 2024
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

# Check if at least one input file argument is provided
if [ $# -lt 1 ]; then
  echo "Error: No input file provided."
  echo "Usage: ./script.sh <input_file1> [<input_file2> ...]"
  exit 1
fi


# Process each input file
for input_file in "$@"; do
  # Check if the input file exists
  if [ ! -f "$input_file" ]; then
    echo "Error: Input file '$input_file' does not exist."
    continue
  fi

  # Initialize variables
  total_tests_run=0
  total_failures=0
  total_errors=0
  total_skipped=0

  # Read the input file line by line
  while IFS= read -r line; do
    # Check if the line contains the test statistics
    if [[ $line =~ ^Tests\ run:\ ([0-9]+).*Failures:\ ([0-9]+).*Errors:\ ([0-9]+).*Skipped:\ ([0-9]+) ]]; then
      # Extract the values from the regex match groups
      tests_run="${BASH_REMATCH[1]}"
      failures="${BASH_REMATCH[2]}"
      errors="${BASH_REMATCH[3]}"
      skipped="${BASH_REMATCH[4]}"

      # Convert values to integers
      tests_run=${tests_run//,}
      failures=${failures//,}
      errors=${errors//,}
      skipped=${skipped//,}

      # Add the values to the totals
      ((total_tests_run += tests_run))
      ((total_failures += failures))
      ((total_errors += errors))
      ((total_skipped += skipped))
    fi
  done < "$input_file"

  # Append the totals to the input file
  echo "" >> "$input_file"
  echo "Total Tests Run: $total_tests_run" >> "$input_file"
  echo "Total Failures: $total_failures" >> "$input_file"
  echo "Total Errors: $total_errors" >> "$input_file"
  echo "Total Skipped: $total_skipped" >> "$input_file"
done
