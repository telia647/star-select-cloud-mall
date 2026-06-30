package com.demo.mall.ai.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Configuration
public class LazyMilvusVectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel, MilvusClientProperties properties) {
        return new LazyMilvusVectorStore(embeddingModel, properties);
    }

    private static final class LazyMilvusVectorStore implements VectorStore {

        private static final Logger log = LoggerFactory.getLogger(LazyMilvusVectorStore.class);
        private static final String GENERIC_UNAVAILABLE_MESSAGE = "向量库暂时不可用，请稍后重试";

        private final EmbeddingModel embeddingModel;
        private final MilvusClientProperties properties;
        private volatile MilvusVectorStore delegate;
        private volatile long unavailableUntil;
        private volatile String lastFailureMessage;

        private LazyMilvusVectorStore(EmbeddingModel embeddingModel, MilvusClientProperties properties) {
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
                    String msg = ex.getMessage() == null ? "" : ex.getMessage();
                    if (msg.contains("Proxy is not ready") || msg.contains("not ready yet")) {
                        this.delegate = null;
                        throw new IllegalStateException("Milvus 正在启动中，请稍后重试", ex);
                    }
                    markUnavailable(ex);
                    throw new IllegalStateException(GENERIC_UNAVAILABLE_MESSAGE, ex);
                }
                return this.delegate;
            }
        }

        private void markUnavailable(RuntimeException ex) {
            this.delegate = null;
            this.unavailableUntil = System.currentTimeMillis() + 30_000;
            this.lastFailureMessage = GENERIC_UNAVAILABLE_MESSAGE;
            log.warn("Milvus unavailable, cooldown 30s: {}", ex.getMessage(), ex);
        }

        private MilvusVectorStore createDelegate() {
            MilvusClientProperties.Client client = properties.getClient();
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(client.getHost())
                    .withPort(client.getPort())
                    .withDatabaseName(properties.getDatabaseName())
                    .withConnectTimeout(client.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                    .withKeepAliveTime(client.getKeepAliveTimeMs(), TimeUnit.MILLISECONDS)
                    .withKeepAliveTimeout(client.getKeepAliveTimeoutMs(), TimeUnit.MILLISECONDS)
                    .withRpcDeadline(client.getRpcDeadlineMs(), TimeUnit.MILLISECONDS)
                    .withSecure(client.isSecure())
                    .withIdleTimeout(client.getIdleTimeoutMs(), TimeUnit.MILLISECONDS)
                    .withAuthorization(client.getUsername(), client.getPassword())
                    .build();

            MilvusVectorStore vectorStore = MilvusVectorStore.builder(new MilvusServiceClient(connectParam), embeddingModel)
                    .initializeSchema(properties.isInitializeSchema())
                    .databaseName(properties.getDatabaseName())
                    .collectionName(properties.getCollectionName())
                    .embeddingDimension(properties.getEmbeddingDimension())
                    .indexType(IndexType.valueOf(properties.getIndexType()))
                    .metricType(MetricType.valueOf(properties.getMetricType()))
                    .indexParameters(properties.getIndexParameters())
                    .build();
            try {
                vectorStore.afterPropertiesSet();
            } catch (Exception ex) {
                throw new IllegalStateException("向量库初始化失败，请确认 Milvus 已启动并且端口可访问", ex);
            }
            return vectorStore;
        }
    }
}
