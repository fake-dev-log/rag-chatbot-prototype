// 스크립트는 MONGO_INITDB_DATABASE 환경 변수에 맞추어 실행됩니다.
db = db.getSiblingDB('prototype');

// 1: 메시지 컬렉션 생성 시 JSON Schema Validator 설정
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
                    description: '메시지 발신자 구분'
                },
                content: {
                    bsonType: ['string', 'object'],
                    description: '텍스트 또는 JSON payload'
                },
                contentType: {
                    bsonType: 'string',
                    description: 'text, image, quick_reply 등'
                },
                sequence: {
                    bsonType: 'long',
                    description: '메시지 순서'
                },
                createdAt: {
                    bsonType: 'date',
                    description: '생성 시각'
                }
            }
        }
    }
});

// 2: 인덱스 생성 (chatId + sequence 순 정렬용)
db.messages.createIndex(
    { chatId: 1, sequence: 1 },
    { name: 'conv_seq_idx' }
);
