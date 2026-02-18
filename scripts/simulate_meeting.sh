#!/bin/bash
# simulate_meeting.sh — Happy-path simulation script
# Usage: ./simulate_meeting.sh [BASE_URL]
#
# This script simulates a basic meeting lifecycle:
#   meeting.started → transcript chunks → meeting.ended
 
BASE_URL="${1:-http://localhost:8080}"
WEBHOOK_URL="$BASE_URL/api/webhooks"

uuid_gen() { uuidgen 2>/dev/null || powershell -NoProfile -Command "[guid]::NewGuid().ToString()"; }

MEETING_ID="50c8940e-1b97-402a-97d6-2708b7feca41"
SESSION_ID="05e57591-d89e-45c9-ae44-08dc1eaad0e0"
ORGANIZER_ID="70c5d391-5bca-4cf3-9907-bec205798adb"
 
echo "=== Sending meeting.started ==="
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
  "event": "meeting.started",
  "meeting": {
    "id": "'$MEETING_ID'",
    "sessionId": "'$SESSION_ID'",
    "title": "Q4 Planning Sync",
    "roomName": "lcfvaa-absxch",
    "status": "LIVE",
    "createdAt": "2024-12-13T06:57:09.736Z",
    "startedAt": "2024-12-13T06:57:09.736Z",
    "organizedBy": {
      "id": "'$ORGANIZER_ID'",
      "name": "Alice Johnson"
    }
  }
}'
echo ""
sleep 1
 
# --- Transcript Chunks ---
SPEAKERS=("Alice Johnson" "Bob Smith" "Alice Johnson")
SPEAKER_IDS=(
  "70c5d391-5bca-4cf3-9907-bec205798adb"
  "82d6e402-6cdb-5df4-a018-19ed2fbce1bc"
  "70c5d391-5bca-4cf3-9907-bec205798adb"
)
CONTENTS=(
  "Alright, let us get started with the Q4 planning."
  "Sure. I have the revenue projections ready to share."
  "Great, go ahead and walk us through the numbers."
)
START_OFFSETS=(2100 5800 9400)
END_OFFSETS=(5200 8900 12600)
 
for i in 0 1 2; do
  SEQ=$((i + 1))
  TRANSCRIPT_ID=$(uuid_gen)
  echo "=== Sending transcript chunk #$SEQ ==="
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
        \"id\": \"${SPEAKER_IDS[$i]}\",
        \"name\": \"${SPEAKERS[$i]}\"
      },
      \"content\": \"${CONTENTS[$i]}\",
      \"startOffset\": \"${START_OFFSETS[$i]}\",
      \"endOffset\": \"${END_OFFSETS[$i]}\",
      \"language\": \"en\"
    }
  }"
  echo ""
  sleep 1
done
 
echo "=== Sending meeting.ended ==="
curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
  "event": "meeting.ended",
  "meeting": {
    "id": "'$MEETING_ID'",
    "sessionId": "'$SESSION_ID'",
    "title": "Q4 Planning Sync",
    "status": "LIVE",
    "createdAt": "2024-12-13T06:57:09.736Z",
    "startedAt": "2024-12-13T06:57:09.736Z",
    "endedAt": "2024-12-13T07:04:37.052Z",
    "organizedBy": {
      "id": "'$ORGANIZER_ID'",
      "name": "Alice Johnson"
    }
  },
  "reason": "HOST_ENDED_MEETING"
}'
echo ""
 
echo "=== Simulation complete ==="
echo "Verify: GET $BASE_URL/api/meetings/$MEETING_ID/sessions/$SESSION_ID/transcripts"