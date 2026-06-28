CREATE TABLE IF NOT EXISTS ai_knowledge_doc (
    id BIGINT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
    embedding_status TINYINT NOT NULL DEFAULT 0 COMMENT '0 pending, 1 indexed, 2 failed',
    last_embedding_error VARCHAR(512) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ai_knowledge_doc_category (category),
    KEY idx_ai_knowledge_doc_status (status, embedding_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id BIGINT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    chunk_no INT NOT NULL,
    content TEXT NOT NULL,
    vector_id VARCHAR(64) NOT NULL,
    token_count INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ai_knowledge_chunk_vector (vector_id),
    KEY idx_ai_knowledge_chunk_doc_id (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ai_conversation_user_id (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_code VARCHAR(16) NOT NULL COMMENT 'USER or ASSISTANT',
    content TEXT NOT NULL,
    references_json JSON NULL,
    tool_result_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ai_message_conversation_id (conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_model_call_log (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NULL,
    user_id BIGINT NULL,
    provider VARCHAR(32) NOT NULL,
    model_name VARCHAR(64) NOT NULL,
    prompt TEXT NOT NULL,
    answer TEXT NULL,
    status TINYINT NOT NULL COMMENT '1 success, 2 failed',
    elapsed_ms BIGINT NOT NULL DEFAULT 0,
    error_message VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ai_model_call_user_time (user_id, created_at),
    KEY idx_ai_model_call_status_time (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_tool_call_log (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NULL,
    user_id BIGINT NULL,
    tool_name VARCHAR(64) NOT NULL,
    arguments_json JSON NOT NULL,
    result_json JSON NULL,
    status TINYINT NOT NULL COMMENT '1 success, 2 failed',
    elapsed_ms BIGINT NOT NULL DEFAULT 0,
    error_message VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ai_tool_call_user_time (user_id, created_at),
    KEY idx_ai_tool_call_tool_time (tool_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
