package com.demo.mall.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.ai.vectorstore.milvus")
public class MilvusClientProperties {

    private String databaseName = "default";
    private String collectionName = "mall_knowledge_chunk";
    private int embeddingDimension = 1024;
    private boolean initializeSchema = true;
    private String indexType = "IVF_FLAT";
    private String metricType = "COSINE";
    private String indexParameters = "{\"nlist\":1024}";
    private Client client = new Client();

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    public boolean isInitializeSchema() { return initializeSchema; }
    public void setInitializeSchema(boolean initializeSchema) { this.initializeSchema = initializeSchema; }
    public String getIndexType() { return indexType; }
    public void setIndexType(String indexType) { this.indexType = indexType; }
    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public String getIndexParameters() { return indexParameters; }
    public void setIndexParameters(String indexParameters) { this.indexParameters = indexParameters; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public static class Client {
        private String host = "localhost";
        private int port = 19530;
        private long connectTimeoutMs = 10_000;
        private long keepAliveTimeMs = 55_000;
        private long keepAliveTimeoutMs = 20_000;
        private long rpcDeadlineMs = 0;
        private long idleTimeoutMs = 86_400_000;
        private boolean secure = false;
        private String username = "root";
        private String password = "milvus";

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public long getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public long getKeepAliveTimeMs() { return keepAliveTimeMs; }
        public void setKeepAliveTimeMs(long keepAliveTimeMs) { this.keepAliveTimeMs = keepAliveTimeMs; }
        public long getKeepAliveTimeoutMs() { return keepAliveTimeoutMs; }
        public void setKeepAliveTimeoutMs(long keepAliveTimeoutMs) { this.keepAliveTimeoutMs = keepAliveTimeoutMs; }
        public long getRpcDeadlineMs() { return rpcDeadlineMs; }
        public void setRpcDeadlineMs(long rpcDeadlineMs) { this.rpcDeadlineMs = rpcDeadlineMs; }
        public long getIdleTimeoutMs() { return idleTimeoutMs; }
        public void setIdleTimeoutMs(long idleTimeoutMs) { this.idleTimeoutMs = idleTimeoutMs; }
        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
