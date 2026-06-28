package com.demo.mall.ai.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Configuration
public class LazyMilvusVectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel,
                                   @Value("${spring.ai.vectorstore.milvus.client.host:localhost}") String host,
                                   @Value("${spring.ai.vectorstore.milvus.client.port:19530}") int port,
                                   @Value("${spring.ai.vectorstore.milvus.database-name:default}") String databaseName,
                                   @Value("${spring.ai.vectorstore.milvus.collection-name:mall_knowledge_chunk}") String collectionName,
                                   @Value("${spring.ai.vectorstore.milvus.embedding-dimension:1024}") int embeddingDimension,
                                   @Value("${spring.ai.vectorstore.milvus.initialize-schema:true}") boolean initializeSchema,
                                   @Value("${spring.ai.vectorstore.milvus.index-type:IVF_FLAT}") String indexType,
                                   @Value("${spring.ai.vectorstore.milvus.metric-type:COSINE}") String metricType,
                                   @Value("${spring.ai.vectorstore.milvus.index-parameters:{\"nlist\":1024}}") String indexParameters,
                                   @Value("${spring.ai.vectorstore.milvus.client.connect-timeout-ms:10000}") long connectTimeoutMs,
                                   @Value("${spring.ai.vectorstore.milvus.client.keep-alive-time-ms:55000}") long keepAliveTimeMs,
                                   @Value("${spring.ai.vectorstore.milvus.client.keep-alive-timeout-ms:20000}") long keepAliveTimeoutMs,
                                   @Value("${spring.ai.vectorstore.milvus.client.rpc-deadline-ms:0}") long rpcDeadlineMs,
                                   @Value("${spring.ai.vectorstore.milvus.client.idle-timeout-ms:86400000}") long idleTimeoutMs,
                                   @Value("${spring.ai.vectorstore.milvus.client.secure:false}") boolean secure,
                                   @Value("${spring.ai.vectorstore.milvus.client.username:root}") String username,
                                   @Value("${spring.ai.vectorstore.milvus.client.password:milvus}") String password) {
        LazyMilvusProperties properties = new LazyMilvusProperties(
                host,
                port,
                databaseName,
                collectionName,
                embeddingDimension,
                initializeSchema,
                indexType,
                metricType,
                indexParameters,
                connectTimeoutMs,
                keepAliveTimeMs,
                keepAliveTimeoutMs,
                rpcDeadlineMs,
                idleTimeoutMs,
                secure,
                username,
                password
        );
        return new LazyMilvusVectorStore(embeddingModel, properties);
    }

    private static final class LazyMilvusVectorStore implements VectorStore {

        private final EmbeddingModel embeddingModel;
        private final LazyMilvusProperties properties;
        private volatile MilvusVectorStore delegate;
        private volatile long unavailableUntil;
        private volatile String lastFailureMessage;

        private LazyMilvusVectorStore(EmbeddingModel embeddingModel, LazyMilvusProperties properties) {
            this.embeddingModel = embeddingModel;
            this.properties = properties;
        }

        @Override
        public String getName() {
            return "milvus";
        }

        @Override
        public void add(List<Document> documents) {
            delegate().add(documents);
        }

        @Override
        public void delete(List<String> idList) {
            delegate().delete(idList);
        }

        @Override
        public void delete(Filter.Expression expression) {
            delegate().delete(expression);
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return delegate().similaritySearch(request);
        }

        @Override
        public <T> Optional<T> getNativeClient() {
            return Optional.empty();
        }

        private MilvusVectorStore delegate() {
            MilvusVectorStore current = this.delegate;
            if (current != null) {
                return current;
            }
            synchronized (this) {
                if (this.delegate != null) {
                    return this.delegate;
                }
                long now = System.currentTimeMillis();
                if (now < this.unavailableUntil) {
                    throw new IllegalStateException(this.lastFailureMessage);
                }
                try {
                    this.delegate = createDelegate();
                } catch (RuntimeException ex) {
                    markUnavailable(ex);
                    throw ex;
                }
                return this.delegate;
            }
        }

        private void markUnavailable(RuntimeException ex) {
            this.delegate = null;
            this.unavailableUntil = System.currentTimeMillis() + 30_000;
            this.lastFailureMessage = ex.getMessage() == null ? "向量库暂时不可用" : ex.getMessage();
        }

        private MilvusVectorStore createDelegate() {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(properties.host())
                    .withPort(properties.port())
                    .withDatabaseName(properties.databaseName())
                    .withConnectTimeout(properties.connectTimeoutMs(), TimeUnit.MILLISECONDS)
                    .withKeepAliveTime(properties.keepAliveTimeMs(), TimeUnit.MILLISECONDS)
                    .withKeepAliveTimeout(properties.keepAliveTimeoutMs(), TimeUnit.MILLISECONDS)
                    .withRpcDeadline(properties.rpcDeadlineMs(), TimeUnit.MILLISECONDS)
                    .withSecure(properties.secure())
                    .withIdleTimeout(properties.idleTimeoutMs(), TimeUnit.MILLISECONDS)
                    .withAuthorization(properties.username(), properties.password())
                    .build();

            MilvusVectorStore vectorStore = MilvusVectorStore.builder(new MilvusServiceClient(connectParam), embeddingModel)
                    .initializeSchema(properties.initializeSchema())
                    .databaseName(properties.databaseName())
                    .collectionName(properties.collectionName())
                    .embeddingDimension(properties.embeddingDimension())
                    .indexType(IndexType.valueOf(properties.indexType()))
                    .metricType(MetricType.valueOf(properties.metricType()))
                    .indexParameters(properties.indexParameters())
                    .build();
            try {
                vectorStore.afterPropertiesSet();
            } catch (Exception ex) {
                throw new IllegalStateException("向量库初始化失败，请确认 Milvus 已启动并且端口可访问", ex);
            }
            return vectorStore;
        }
    }

    private record LazyMilvusProperties(String host,
                                        int port,
                                        String databaseName,
                                        String collectionName,
                                        int embeddingDimension,
                                        boolean initializeSchema,
                                        String indexType,
                                        String metricType,
                                        String indexParameters,
                                        long connectTimeoutMs,
                                        long keepAliveTimeMs,
                                        long keepAliveTimeoutMs,
                                        long rpcDeadlineMs,
                                        long idleTimeoutMs,
                                        boolean secure,
                                        String username,
                                        String password) {
    }
}
