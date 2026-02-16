#!/bin/bash
# simulate_meeting_edge_cases.sh
# Usage: ./simulate_meeting_edge_cases.sh [BASE_URL]

BASE_URL="${1:-http://localhost:8080}"
WEBHOOK_URL="$BASE_URL/api/webhooks"

MEETING_ID="50c8940e-1b97-402a-97d6-2708b7feca41"
SESSION_ID="05e57591-d89e-45c9-ae44-08dc1eaad0e0"
ORGANIZER_ID="70c5d391-5bca-4cf3-9907-bec205798adb"

function send_started() {
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d "{
  \"event\": \"meeting.started\",
  \"meeting\": {
    \"id\": \"$MEETING_ID\",
    \"sessionId\": \"$SESSION_ID\",
    \"title\": \"Edge Case Test\",
    \"roomName\": \"test-room\",
    \"status\": \"LIVE\",
    \"createdAt\": \"2024-12-13T06:57:09.736Z\",
    \"startedAt\": \"2024-12-13T06:57:09.736Z\",
    \"organizedBy\": {
      \"id\": \"$ORGANIZER_ID\",
      \"name\": \"Alice Johnson\"
    }
  }
}"
echo ""
}

function send_ended() {
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d "{
  \"event\": \"meeting.ended\",
  \"meeting\": {
    \"id\": \"$MEETING_ID\",
    \"sessionId\": \"$SESSION_ID\",
    \"title\": \"Edge Case Test\",
    \"status\": \"LIVE\",
    \"createdAt\": \"2024-12-13T06:57:09.736Z\",
    \"startedAt\": \"2024-12-13T06:57:09.736Z\",
    \"endedAt\": \"2024-12-13T07:04:37.052Z\",
    \"organizedBy\": {
      \"id\": \"$ORGANIZER_ID\",
      \"name\": \"Alice Johnson\"
    }
  },
  \"reason\": \"HOST_ENDED_MEETING\"
}"
echo ""
}

function send_transcript() {
  TRANSCRIPT_ID=$1
  SEQ=$2
  CONTENT=$3

curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d "{
  \"event\": \"meeting.transcript\",
  \"meeting\": {
    \"id\": \"$MEETING_ID\",
    \"sessionId\": \"$SESSION_ID\"
  },
  \"data\": {
    \"transcriptId\": \"$TRANSCRIPT_ID\",
    \"sequenceNumber\": $SEQ,
    \"speaker\": {
      \"id\": \"$ORGANIZER_ID\",
      \"name\": \"Alice Johnson\"
    },
    \"content\": \"$CONTENT\",
    \"startOffset\": \"00:00:01.000\",
    \"endOffset\": \"00:00:02.000\",
    \"language\": \"en\"
  }
}"
echo ""
}

############################################################
echo "===== SCENARIO 1: Duplicate Transcript Chunk ====="
############################################################
send_started
TID=$(uuidgen)
send_transcript "$TID" 1 "Duplicate test"
echo "Sending duplicate..."
send_transcript "$TID" 1 "Duplicate test"
send_ended
sleep 2

############################################################
echo "===== SCENARIO 2: Out-of-Order Transcript Delivery ====="
############################################################
send_started
send_transcript "$(uuidgen)" 3 "Third message"
send_transcript "$(uuidgen)" 1 "First message"
send_transcript "$(uuidgen)" 2 "Second message"
send_ended
sleep 2

############################################################
echo "===== SCENARIO 3: Transcript After meeting.ended ====="
############################################################
send_started
send_transcript "$(uuidgen)" 1 "Before ending"
send_ended
echo "Sending transcript AFTER meeting ended..."
send_transcript "$(uuidgen)" 2 "Should be rejected or ignored"
sleep 2

############################################################
echo "===== SCENARIO 4: meeting.ended Without meeting.started ====="
############################################################
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d "{
  \"event\": \"meeting.ended\",
  \"meeting\": {
    \"id\": \"$(uuidgen)\",
    \"sessionId\": \"$(uuidgen)\",
    \"title\": \"Ghost meeting\",
    \"status\": \"LIVE\",
    \"createdAt\": \"2024-12-13T06:57:09.736Z\",
    \"startedAt\": \"2024-12-13T06:57:09.736Z\",
    \"endedAt\": \"2024-12-13T07:04:37.052Z\",
    \"organizedBy\": {
      \"id\": \"$ORGANIZER_ID\",
      \"name\": \"Alice Johnson\"
    }
  },
  \"reason\": \"HOST_ENDED_MEETING\"
}"
echo ""
sleep 2

############################################################
echo "===== SCENARIO 5: Concurrent Sessions For Same Meeting ====="
############################################################
SESSION_A=$(uuidgen)
SESSION_B=$(uuidgen)

for SESSION in $SESSION_A $SESSION_B; do
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d "{
  \"event\": \"meeting.started\",
  \"meeting\": {
    \"id\": \"$MEETING_ID\",
    \"sessionId\": \"$SESSION\",
    \"title\": \"Concurrent Session\",
    \"roomName\": \"room\",
    \"status\": \"LIVE\",
    \"createdAt\": \"2024-12-13T06:57:09.736Z\",
    \"startedAt\": \"2024-12-13T06:57:09.736Z\",
    \"organizedBy\": {
      \"id\": \"$ORGANIZER_ID\",
      \"name\": \"Alice Johnson\"
    }
  }
}"
done

echo "Both sessions started simultaneously."
sleep 2

############################################################
echo "===== SCENARIO 6: Duplicate meeting.started ====="
############################################################
send_started
echo "Sending duplicate meeting.started..."
send_started
sleep 2

############################################################
echo "===== SCENARIO 7: Corrupted Payload (Missing Fields) ====="
############################################################
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "event": "meeting.transcript",
    "meeting": {
      "id": "'$MEETING_ID'"
    }
  }'
echo ""
sleep 2

############################################################
echo "===== SCENARIO 8: Large Transcript Payload ====="
############################################################
LARGE_CONTENT=$(printf 'A%.0s' {1..10000})
send_started
send_transcript "$(uuidgen)" 1 "$LARGE_CONTENT"
send_ended

echo "===== Edge Case Simulation Complete ====="
