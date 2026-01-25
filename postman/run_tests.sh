#!/bin/bash

# Configuration
COLLECTION_FILE="cot_chat_test_collection.json"
BASE_URL="http://localhost:8084"

echo "🚀 Starting Newman Tests for StellarMIND Streaming Chat API..."
echo "📍 API Base URL: $BASE_URL"
echo ""

# Run tests using npx newman
npx newman run "$COLLECTION_FILE" \
    --reporters cli \
    --bail

# Check execution status
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All tests passed successfully!"
else
    echo ""
    echo "❌ Some tests failed. Please check the logs above."
    exit 1
fi
