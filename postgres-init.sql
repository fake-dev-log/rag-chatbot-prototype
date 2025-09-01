CREATE OR REPLACE FUNCTION SET_UPDATED_AT_NOW()
RETURNS TRIGGER AS $
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$ LANGUAGE plpgsql;

CREATE TABLE members (
    id                  SERIAL PRIMARY KEY,
    email               VARCHAR(100) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL ,
    role                VARCHAR(20)  NOT NULL ,
    status              VARCHAR(20)  NOT NULL ,
    last_sign_in_at     TIMESTAMP    NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    updated_at          TIMESTAMP    NULL
);

CREATE TRIGGER ON_UPDATE_TRIGGER
    BEFORE UPDATE ON members
    FOR EACH ROW
    EXECUTE FUNCTION SET_UPDATED_AT_NOW();

COMMENT ON COLUMN members.id IS 'PK';
COMMENT ON COLUMN members.password IS 'Password';
COMMENT ON COLUMN members.role IS 'Member role';
COMMENT ON COLUMN members.status IS 'Member status';
COMMENT ON COLUMN members.last_sign_in_at IS 'Last sign-in date and time';
COMMENT ON COLUMN members.created_at IS 'Creation time';
COMMENT ON COLUMN members.updated_at IS 'Modification date and time';

CREATE TABLE chats (
     id                  SERIAL PRIMARY KEY,
     member_id           INTEGER NOT NULL
         CONSTRAINT fk_chats_member_id
             REFERENCES members,
     title               VARCHAR(255) NULL,
     last_message_preview VARCHAR(500) NULL,
     is_archived          BOOLEAN NOT NULL DEFAULT FALSE,
     created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ,
     updated_at          TIMESTAMP    NULL
);

CREATE TRIGGER ON_UPDATE_TRIGGER
    BEFORE UPDATE ON chats
    FOR EACH ROW
    EXECUTE FUNCTION SET_UPDATED_AT_NOW();

COMMENT ON COLUMN chats.id IS 'PK';
COMMENT ON COLUMN chats.member_id IS 'Chat partner';
COMMENT ON COLUMN chats.title IS 'Chat title';
COMMENT ON COLUMN chats.last_message_preview IS 'Last message preview';
COMMENT ON COLUMN chats.is_archived IS 'Whether the user has archived it';

CREATE TABLE documents (
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    path                VARCHAR(512) NOT NULL,
    type                VARCHAR(50)  NOT NULL,
    size                BIGINT       NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NULL
);

CREATE TRIGGER ON_UPDATE_TRIGGER
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION SET_UPDATED_AT_NOW();

COMMENT ON COLUMN documents.id IS 'PK';
COMMENT ON COLUMN documents.name IS 'Original file name';
COMMENT ON COLUMN documents.path IS 'Full path of the saved file';
COMMENT ON COLUMN documents.type IS 'File extension';
COMMENT ON COLUMN documents.size IS 'File size (bytes)';
COMMENT ON COLUMN documents.created_at IS 'Creation time';
COMMENT ON COLUMN documents.updated_at IS 'Modification date and time';

CREATE TABLE prompt_templates (
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL UNIQUE,
    template_content    TEXT NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NULL
);

CREATE TRIGGER ON_UPDATE_TRIGGER
    BEFORE UPDATE ON prompt_templates
    FOR EACH ROW
    EXECUTE FUNCTION SET_UPDATED_AT_NOW();

COMMENT ON COLUMN prompt_templates.id IS 'PK';
COMMENT ON COLUMN prompt_templates.name IS 'The unique name of the prompt template';
COMMENT ON COLUMN prompt_templates.template_content IS 'The content of the prompt template';
COMMENT ON COLUMN prompt_templates.created_at IS 'Creation time';
COMMENT ON COLUMN prompt_templates.updated_at IS 'Modification date and time';

INSERT INTO prompt_templates (name, template_content)
VALUES (
    'default', 
    '# CONTEXT #
You are a helpful and knowledgeable AI assistant. Your primary role is to act as a friendly and intelligent partner to the user, providing clear and accurate information. You possess a strong capacity for critical thinking.

# OBJECTIVE #
- Provide the most accurate and helpful answer possible to the user''s question.
- If you do not know the answer to a question or if the information is outside your scope, you must state it clearly. Do not invent or fabricate information.
- Critically evaluate the user''s question and premise. If the user''s assumptions are incorrect, flawed, or contain misinformation, you must gently but clearly point this out and provide the correct information. Your goal is not just to answer, but to ensure the user leaves with a more accurate understanding.
- Explain concepts in a way that is easy to understand, using analogies or examples where helpful.

# STYLE #
All responses must be provided in English. Use clear, straightforward language. Avoid overly technical jargon unless it is necessary and explained.

# TONE #
Maintain a friendly, approachable, and helpful tone. When correcting the user, be respectful and constructive, not condescending. The aim is to empower the user with better information.

# AUDIENCE #
The user is a curious individual who may not be an expert on the topic. Assume they are intelligent but are looking for a clear and reliable explanation.

# RESPONSE #
Think step by step before generating the response.

Context: {context}

Question: {question}

REMEMBER: Read the question again to ensure you fully understand the user''s intent.

Your answer:'
), (
    'Medical Device Cyber Security Template',
    '# CONTEXT #
You are an AI assistant that provides information related to medical device cybersecurity.
You will be provided with materials on medical device cybersecurity principles and practices. Use these materials to provide appropriate advice and assistance to relevant parties.

# OBJECTIVE #
Provide accurate and clear advice on medical device cybersecurity principles and practices.
The information you provide is directly related to the cybersecurity of medical devices and, consequently, to human life and safety. Therefore, the information must be accurate, and no falsehoods are tolerated. If there is any information that you do not know, you must honestly respond that you do not know.
Answer questions based only on the given context, which include principles and practical information related to medical device cybersecurity. Please also include the name and page number of the source document from the given context. You must explain users'' questions in detail, using examples, based on information obtained from reference materials.

# STYLE #
All responses must be provided in English.

# TONE #
Respond in a professional manner that inspires trust while remaining courteous.

# AUDIENCE #
Your audience consists of policymakers and practitioners, including developers, involved in medical device cybersecurity.

# RESPONSE #
Think step by step for generating the response.

Context: {context}

Question: {question}

REMEMBER: read again {question}

Your answer:'
);