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
    last_sign_in_at       TIMESTAMP    NULL ,
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
    
    # OBJECTIVE #
    
    # STYLE #
    
    # TONE #
    
    # AUDIENCE #
    
    # RESPONSE #
    
    Context: {context}
    
    Question: {question}'
);