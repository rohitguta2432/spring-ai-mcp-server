-- Create table only if it doesn't already exist
CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
                                                     conversation_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, timestamp)
    );

-- Ensure index exists on conversation_id
CREATE INDEX IF NOT EXISTS idx_spring_ai_chat_memory_conversation_id
    ON spring_ai_chat_memory(conversation_id);

-- Ensure index exists on timestamp
CREATE INDEX IF NOT EXISTS idx_spring_ai_chat_memory_timestamp
    ON spring_ai_chat_memory(timestamp);
