CREATE OR REPLACE FUNCTION SET_UPDATED_AT_NOW()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE members (
    id                  SERIAL PRIMARY KEY,
    email               VARCHAR(100) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL ,
    role                VARCHAR(20)  NOT NULL ,
    status              VARCHAR(20)  NOT NULL ,
    last_login_at       TIMESTAMP    NULL ,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    updated_at          TIMESTAMP    NULL
);

CREATE TRIGGER ON_UPDATE_TRIGGER
    BEFORE UPDATE ON members
    FOR EACH ROW
    EXECUTE FUNCTION SET_UPDATED_AT_NOW();

COMMENT ON COLUMN members.id IS 'PK';
COMMENT ON COLUMN members.password IS '비밀번호';
COMMENT ON COLUMN members.role IS '회원 역할';
COMMENT ON COLUMN members.status IS '회원 상태';
COMMENT ON COLUMN members.last_login_at IS '마지막 로그인 일시';
COMMENT ON COLUMN members.created_at IS '생성시간';
COMMENT ON COLUMN members.updated_at IS '수정 일시';

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
COMMENT ON COLUMN chats.member_id IS '대화 상대';
COMMENT ON COLUMN chats.title IS '대화 제목';
COMMENT ON COLUMN chats.last_message_preview IS '최근 메시지 미리보기';
COMMENT ON COLUMN chats.is_archived IS '사용자가 보관(아카이브)했는지 여부';