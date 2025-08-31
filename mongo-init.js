// This script is executed according to the MONGO_INITDB_DATABASE environment variable.
db = db.getSiblingDB('prototype');

// 1: Set JSON Schema Validator when creating the messages collection
db.createCollection('messages', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['chatId', 'sender', 'content', 'sequence', 'createdAt'],
            properties: {
                chatId: {
                    bsonType: 'long',
                    description: 'RDB chat.id'
                },
                sender: {
                    enum: ['USER', 'BOT'],
                    description: 'Distinguishes the message sender'
                },
                content: {
                    bsonType: ['string', 'object'],
                    description: 'Text or JSON payload'
                },
                contentType: {
                    bsonType: 'string',
                    description: 'e.g., text, image, quick_reply'
                },
                sequence: {
                    bsonType: 'long',
                    description: 'Message order'
                },
                createdAt: {
                    bsonType: 'date',
                    description: 'Creation time'
                }
            }
        }
    }
});

// 2: Create index (for sorting by chatId + sequence)
db.messages.createIndex(
    { chatId: 1, sequence: 1 },
    { name: 'conv_seq_idx' }
);