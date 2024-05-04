#!/bin/bash

# Define the location of the Maven wrapper and the jar file
MVNW_PATH="./mvnw"
JAR_FILE="target/todo-0.0.1-SNAPSHOT.jar"
LOG_DIR="target"
LOG_FILE="${LOG_DIR}/log.txt"

# Ensure Maven wrapper is executable
chmod +x $MVNW_PATH

# Build the project
$MVNW_PATH clean package

# Array to hold all the start times
declare -a startTimes

# Loop to start the application 100 times
for i in {1..100}
do
    echo "Iteration $i"

    # Start the application in the background and redirect output to log file
    java -jar $JAR_FILE > $LOG_FILE 2>&1 &
    PID=$!

    # Wait for a few seconds to let the application initialize
    sleep 0.5

    # Kill the application
    kill $PID
    wait $PID

    # Read the last line from the log file
    lastLine=$(tail -1 $LOG_FILE)
    REGEX="Curryful server started in (.*)ms"

    # Check if the line matches the expected startup message
    if [[ $lastLine =~ $REGEX ]]; then
        startTimes+=(${BASH_REMATCH[1]})
    else
        echo "Failed to extract start time in iteration $i"
    fi

    # Clear the log file to avoid confusion in the next iteration
    > $LOG_FILE
done

# Calculate the average start time
total=0
count=${#startTimes[@]}

for time in "${startTimes[@]}"
do
    total=$(echo "$total + $time" | bc)
done

if [ $count -gt 0 ]; then
    average=$(echo "scale=2; $total / $count" | bc)
    echo "Average cold start time: $average ms"
else
    echo "No valid start times recorded."
fi
