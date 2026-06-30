package com.demo.mall.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.ai.config.AiProperties;
import com.demo.mall.ai.dto.KnowledgeDocRequest;
import com.demo.mall.ai.dto.KnowledgeDocResponse;
import com.demo.mall.ai.dto.KnowledgeSyncResultResponse;
import com.demo.mall.ai.entity.AiKnowledgeChunk;
import com.demo.mall.ai.entity.AiKnowledgeDoc;
import com.demo.mall.ai.mapper.AiKnowledgeChunkMapper;
import com.demo.mall.ai.mapper.AiKnowledgeDocMapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    public static final int DOC_DISABLED = 0;
    public static final int DOC_ENABLED = 1;
    public static final int EMBEDDING_PENDING = 0;
    public static final int EMBEDDING_INDEXED = 1;
    public static final int EMBEDDING_FAILED = 2;

    private final AiKnowledgeDocMapper docMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final KnowledgeChunker chunker;
    private final VectorStore vectorStore;
    private final AiProperties aiProperties;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public KnowledgeService(AiKnowledgeDocMapper docMapper,
                            AiKnowledgeChunkMapper chunkMapper,
                            KnowledgeChunker chunker,
                            VectorStore vectorStore,
                            AiProperties aiProperties,
                            JdbcTemplate jdbcTemplate,
                            TransactionTemplate transactionTemplate) {
        this.docMapper = docMapper;
        this.chunkMapper = chunkMapper;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
        this.aiProperties = aiProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    public List<KnowledgeDocResponse> list(String keyword, String category, Integer status) {
        return docMapper.selectList(new LambdaQueryWrapper<AiKnowledgeDoc>()
                        .like(keyword != null && !keyword.isBlank(), AiKnowledgeDoc::getTitle, keyword)
                        .eq(category != null && !category.isBlank(), AiKnowledgeDoc::getCategory, category)
                        .eq(status != null, AiKnowledgeDoc::getStatus, status)
                        .orderByDesc(AiKnowledgeDoc::getUpdatedAt))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public KnowledgeDocResponse detail(Long id) {
        return toResponse(getDoc(id));
    }

    @Transactional
    public KnowledgeDocResponse create(Long operatorId, KnowledgeDocRequest request) {
        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setTitle(request.title().trim());
        doc.setCategory(request.category().trim());
        doc.setContent(request.content().trim());
        doc.setStatus(normalizeStatus(request.status()));
        doc.setEmbeddingStatus(EMBEDDING_PENDING);
        doc.setCreatedBy(operatorId);
        doc.setUpdatedBy(operatorId);
        docMapper.insert(doc);
        return toResponse(doc);
    }

    @Transactional
    public KnowledgeDocResponse update(Long operatorId, Long id, KnowledgeDocRequest request) {
        AiKnowledgeDoc doc = getDoc(id);
        doc.setTitle(request.title().trim());
        doc.setCategory(request.category().trim());
        doc.setContent(request.content().trim());
        doc.setStatus(normalizeStatus(request.status()));
        doc.setEmbeddingStatus(EMBEDDING_PENDING);
        doc.setLastEmbeddingError(null);
        doc.setUpdatedBy(operatorId);
        docMapper.updateById(doc);
        return toResponse(doc);
    }

    @Transactional
    public void delete(Long id) {
        deleteChunks(id);
        docMapper.deleteById(id);
    }

    public KnowledgeDocResponse index(Long id) {
        AiKnowledgeDoc doc = getDoc(id);
        List<Document> documents;
        try {
            documents = transactionTemplate.execute(status -> rebuildChunksAndCollect(doc));
        } catch (RuntimeException ex) {
            markIndexFailed(doc, ex);
            return toResponse(doc);
        }
        if (documents == null || documents.isEmpty()) {
            return toResponse(doc);
        }
        try {
            vectorStore.add(documents);
            markIndexSucceeded(doc);
        } catch (RuntimeException ex) {
            markIndexFailed(doc, ex);
        }
        return toResponse(doc);
    }

    private List<Document> rebuildChunksAndCollect(AiKnowledgeDoc doc) {
        Long id = doc.getId();
        deleteChunks(id);
        List<String> parts = chunker.split(
                doc.getContent(),
                aiProperties.getRag().getChunkSize(),
                aiProperties.getRag().getChunkOverlap()
        );
        if (parts.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "knowledge content is empty");
        }
        List<Document> documents = new java.util.ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setDocId(id);
            chunk.setChunkNo(i);
            chunk.setContent(parts.get(i));
            chunk.setVectorId("doc-" + id + "-chunk-" + i);
            chunk.setTokenCount(parts.get(i).length());
            chunkMapper.insert(chunk);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("docId", id);
            metadata.put("docTitle", doc.getTitle());
            metadata.put("category", doc.getCategory());
            metadata.put("status", doc.getStatus());
            metadata.put("chunkNo", chunk.getChunkNo());
            documents.add(new Document(chunk.getVectorId(), chunk.getContent(), metadata));
        }
        return documents;
    }

    private void markIndexSucceeded(AiKnowledgeDoc doc) {
        transactionTemplate.executeWithoutResult(status -> {
            doc.setEmbeddingStatus(EMBEDDING_INDEXED);
            doc.setLastEmbeddingError(null);
            docMapper.updateById(doc);
        });
    }

    private void markIndexFailed(AiKnowledgeDoc doc, RuntimeException ex) {
        transactionTemplate.executeWithoutResult(status -> {
            doc.setEmbeddingStatus(EMBEDDING_FAILED);
            doc.setLastEmbeddingError(trim(errorMessage(ex), 512));
            docMapper.updateById(doc);
        });
    }

    public KnowledgeSyncResultResponse syncProducts(Long operatorId) {
        List<ProductKnowledgeRow> rows;
        try {
            rows = listProductRows();
        } catch (DataAccessException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "同步商品知识失败：" + trim(errorMessage(ex), 240));
        }
        List<KnowledgeDocResponse> items = new java.util.ArrayList<>();
        int created = 0;
        int updated = 0;
        int failed = 0;

        if (rows.isEmpty()) {
            UpsertResult r = upsertProductDoc(operatorId, "商城商品总览", "当前商城暂无已上架商品。");
            KnowledgeDocResponse indexed = index(r.response().id());
            items.add(indexed);
            if (indexed.embeddingStatus() == EMBEDDING_FAILED) failed++;
            else if (r.created()) created++; else updated++;
            return new KnowledgeSyncResultResponse(created, updated, failed, items);
        }

        UpsertResult overview = upsertProductDoc(operatorId, "商城商品总览", buildCatalogOverview(rows));
        KnowledgeDocResponse overviewIndexed = index(overview.response().id());
        items.add(overviewIndexed);
        if (overviewIndexed.embeddingStatus() == EMBEDDING_FAILED) failed++;
        else if (overview.created()) created++; else updated++;

        var categoryMap = rows.stream()
                .collect(Collectors.groupingBy(ProductKnowledgeRow::categoryName, java.util.TreeMap::new, Collectors.toList()));
        for (var entry : categoryMap.entrySet()) {
            UpsertResult r = upsertProductDoc(operatorId, "商品分类-" + entry.getKey(), buildCategoryDoc(entry.getKey(), entry.getValue()));
            KnowledgeDocResponse indexed = index(r.response().id());
            items.add(indexed);
            if (indexed.embeddingStatus() == EMBEDDING_FAILED) failed++;
            else if (r.created()) created++; else updated++;
        }
        for (ProductKnowledgeRow row : rows) {
            UpsertResult r = upsertProductDoc(operatorId, "商品-" + row.productName(), buildProductDoc(row));
            KnowledgeDocResponse indexed = index(r.response().id());
            items.add(indexed);
            if (indexed.embeddingStatus() == EMBEDDING_FAILED) failed++;
            else if (r.created()) created++; else updated++;
        }
        return new KnowledgeSyncResultResponse(created, updated, failed, items);
    }

    private record UpsertResult(KnowledgeDocResponse response, boolean created) {
    }

    private UpsertResult upsertProductDoc(Long operatorId, String title, String content) {
        AiKnowledgeDoc existing = docMapper.selectOne(new LambdaQueryWrapper<AiKnowledgeDoc>()
                .eq(AiKnowledgeDoc::getCategory, "商品知识")
                .eq(AiKnowledgeDoc::getTitle, title)
                .last("LIMIT 1"));
        if (existing == null) {
            AiKnowledgeDoc doc = new AiKnowledgeDoc();
            doc.setTitle(title);
            doc.setCategory("商品知识");
            doc.setContent(content);
            doc.setStatus(DOC_ENABLED);
            doc.setEmbeddingStatus(EMBEDDING_PENDING);
            doc.setCreatedBy(operatorId);
            doc.setUpdatedBy(operatorId);
            docMapper.insert(doc);
            return new UpsertResult(toResponse(doc), true);
        }
        existing.setContent(content);
        existing.setStatus(DOC_ENABLED);
        existing.setEmbeddingStatus(EMBEDDING_PENDING);
        existing.setLastEmbeddingError(null);
        existing.setUpdatedBy(operatorId);
        docMapper.updateById(existing);
        return new UpsertResult(toResponse(existing), false);
    }

    private List<ProductKnowledgeRow> listProductRows() {
        boolean hasShopTable = hasTable("mall_product", "pms_shop");
        boolean hasShopId = hasColumn("mall_product", "pms_product", "shop_id");
        String shopNameSelect = hasShopTable && hasShopId ? "COALESCE(s.name, '')" : "''";
        String serviceTagsSelect = hasShopTable && hasShopId ? "COALESCE(s.service_tags, '')" : "''";
        String shopJoin = hasShopTable && hasShopId ? "LEFT JOIN mall_product.pms_shop s ON s.id = p.shop_id" : "";

        return jdbcTemplate.query(
                String.format("""
                SELECT p.id AS product_id,
                       p.name AS product_name,
                       p.subtitle,
                       c.name AS category_name,
                       %s AS shop_name,
                       %s AS service_tags,
                       sku.id AS sku_id,
                       sku.sku_code,
                       CAST(sku.spec_json AS CHAR) AS spec_json,
                       sku.price
                FROM mall_product.pms_product p
                JOIN mall_product.pms_category c ON c.id = p.category_id
                %s
                JOIN mall_product.pms_sku sku ON sku.product_id = p.id
                WHERE p.status = 1 AND sku.status = 1 AND c.status = 1
                ORDER BY c.sort ASC, p.id ASC, sku.price ASC
                """, shopNameSelect, serviceTagsSelect, shopJoin),
                (rs, rowNum) -> new ProductKnowledgeRow(
                        rs.getLong("product_id"),
                        rs.getString("product_name"),
                        rs.getString("subtitle"),
                        rs.getString("category_name"),
                        rs.getString("shop_name"),
                        rs.getString("service_tags"),
                        rs.getLong("sku_id"),
                        rs.getString("sku_code"),
                        rs.getString("spec_json"),
                        rs.getBigDecimal("price")
                )
        );
    }

    private boolean hasTable(String schema, String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """,
                Integer.class,
                schema,
                table
        );
        return count != null && count > 0;
    }

    private boolean hasColumn(String schema, String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ? AND column_name = ?
                """,
                Integer.class,
                schema,
                table,
                column
        );
        return count != null && count > 0;
    }

    private String buildCatalogOverview(List<ProductKnowledgeRow> rows) {
        Map<String, List<ProductKnowledgeRow>> byCategory = rows.stream()
                .collect(Collectors.groupingBy(ProductKnowledgeRow::categoryName, java.util.TreeMap::new, Collectors.toList()));
        ProductKnowledgeRow cheapest = rows.stream()
                .min(java.util.Comparator.comparing(ProductKnowledgeRow::price))
                .orElse(rows.get(0));
        StringBuilder builder = new StringBuilder();
        builder.append("商城当前上架商品覆盖以下分类：\n");
        byCategory.forEach((category, categoryRows) -> {
            String products = categoryRows.stream()
                    .map(ProductKnowledgeRow::productName)
                    .distinct()
                    .collect(Collectors.joining("、"));
            builder.append("- ").append(category).append("：").append(products).append("\n");
        });
        builder.append("当前最低价商品是 ")
                .append(cheapest.productName())
                .append("，SKU ")
                .append(cheapest.skuCode())
                .append("，价格 ")
                .append(cheapest.price())
                .append(" 元。\n");
        builder.append("用户询问商城有什么、有哪些商品、有哪些手机、最便宜商品时，可以基于以上分类和商品回答。");
        return builder.toString();
    }

    private String buildCategoryDoc(String category, List<ProductKnowledgeRow> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("分类：").append(category).append("\n");
        rows.stream()
                .collect(Collectors.groupingBy(ProductKnowledgeRow::productId, java.util.LinkedHashMap::new, Collectors.toList()))
                .forEach((productId, skuRows) -> {
                    ProductKnowledgeRow first = skuRows.get(0);
                    Optional<BigDecimal> minPrice = skuRows.stream().map(ProductKnowledgeRow::price).min(BigDecimal::compareTo);
                    builder.append("- 商品：").append(first.productName())
                            .append("。卖点：").append(nullToEmpty(first.subtitle()))
                            .append("。店铺：").append(nullToEmpty(first.shopName()))
                            .append("。最低价：").append(minPrice.orElse(BigDecimal.ZERO)).append(" 元。")
                            .append("SKU：")
                            .append(skuRows.stream()
                                    .map(row -> row.skuCode() + " " + row.price() + "元 " + nullToEmpty(row.specJson()))
                                    .collect(Collectors.joining("；")))
                            .append("\n");
                });
        return builder.toString();
    }

    private String buildProductDoc(ProductKnowledgeRow row) {
        return "商品：" + row.productName() + "\n"
                + "分类：" + row.categoryName() + "\n"
                + "店铺：" + nullToEmpty(row.shopName()) + "\n"
                + "服务：" + nullToEmpty(row.serviceTags()) + "\n"
                + "卖点：" + nullToEmpty(row.subtitle()) + "\n"
                + "SKU：" + row.skuCode() + "\n"
                + "规格：" + nullToEmpty(row.specJson()) + "\n"
                + "价格：" + row.price() + " 元\n"
                + "当用户询问该商品、价格、规格、所属分类或店铺时，可以使用这些信息回答。";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void deleteChunks(Long docId) {
        try {
            vectorStore.delete(new org.springframework.ai.vectorstore.filter.Filter.Expression(
                    org.springframework.ai.vectorstore.filter.Filter.ExpressionType.EQ,
                    new org.springframework.ai.vectorstore.filter.Filter.Key("docId"),
                    new org.springframework.ai.vectorstore.filter.Filter.Value(docId)));
        } catch (RuntimeException ex) {
            List<AiKnowledgeChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<AiKnowledgeChunk>()
                    .eq(AiKnowledgeChunk::getDocId, docId));
            if (!chunks.isEmpty()) {
                vectorStore.delete(chunks.stream().map(AiKnowledgeChunk::getVectorId).toList());
            }
        }
        chunkMapper.delete(new LambdaQueryWrapper<AiKnowledgeChunk>().eq(AiKnowledgeChunk::getDocId, docId));
    }

    private AiKnowledgeDoc getDoc(Long id) {
        AiKnowledgeDoc doc = docMapper.selectById(id);
        if (doc == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "knowledge doc not found");
        }
        return doc;
    }

    private int normalizeStatus(Integer status) {
        return status == null || status != DOC_DISABLED ? DOC_ENABLED : DOC_DISABLED;
    }

    private KnowledgeDocResponse toResponse(AiKnowledgeDoc doc) {
        return new KnowledgeDocResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getCategory(),
                doc.getContent(),
                doc.getStatus(),
                doc.getEmbeddingStatus(),
                EMBEDDING_INDEXED == doc.getEmbeddingStatus() ? null : doc.getLastEmbeddingError(),
                doc.getUpdatedAt()
        );
    }

    private String trim(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    private String errorMessage(RuntimeException ex) {
        StringBuilder builder = new StringBuilder(ex.getClass().getSimpleName());
        if (ex.getMessage() != null) {
            builder.append(": ").append(ex.getMessage());
        }
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            builder.append("; cause=").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
        }
        return builder.toString();
    }

    private String errorMessage(DataAccessException ex) {
        StringBuilder builder = new StringBuilder(ex.getClass().getSimpleName());
        if (ex.getMessage() != null) {
            builder.append(": ").append(ex.getMessage());
        }
        Throwable cause = ex.getMostSpecificCause();
        if (cause != null && cause.getMessage() != null) {
            builder.append("; cause=").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
        }
        return builder.toString();
    }

    private record ProductKnowledgeRow(Long productId,
                                       String productName,
                                       String subtitle,
                                       String categoryName,
                                       String shopName,
                                       String serviceTags,
                                       Long skuId,
                                       String skuCode,
                                       String specJson,
                                       BigDecimal price) {
    }
}
