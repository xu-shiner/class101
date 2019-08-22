import com.google.gson.Gson;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pojo.Item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @创建人 xgh
 * @创建时间 2019/8/209:19
 * @描述
 */
public class RestEsManager {
    private RestHighLevelClient client = null;

    @Before
    public void init() {
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9201, "http"),
                        new HttpHost("localhost", 9202, "http"),
                        new HttpHost("localhost", 9203, "http")));

    }

    Gson gson = new Gson();

    @Test
    public void addDocument() throws IOException {
        //创建一个item对象
        Item item = new Item(1L, "小米6X手机", "手机", "小米", 1299.0, "xiaomi6s");
        //创建一个文本请求对象
        IndexRequest Request = new IndexRequest("leyou", "item", item.getId().toString());
        //使用gson将item对象转为json字符串
        String jsonString = gson.toJson(item);
        Request.source(jsonString, XContentType.JSON);
        client.index(Request, RequestOptions.DEFAULT);
    }

    @Test
    public void delDocument() throws IOException {
        //创建一个文本请求对象
        DeleteRequest Request = new DeleteRequest("leyou", "item", "1");
        client.delete(Request, RequestOptions.DEFAULT);
    }

    @Test
    public void addDocumentBulk() throws IOException {
        //准备文档数据
        ArrayList<Item> list = new ArrayList<Item>();
        list.add(new Item(1L, "小米6X手机", "手机", "小米", 1299.0, "xiaomi6s"));
        list.add(new Item(2L, "华为p30手机", "手机", "华为", 3299.0, "sdfhe"));
        list.add(new Item(3L, "apple5se手机", "手机", "苹果", 2299.0, "qrgjry"));
        list.add(new Item(4L, "小米7X手机", "手机", "小米", 2888.0, "eiert4"));
        list.add(new Item(5L, "荣耀V10", "手机", "荣耀", 1799.0, "kgfgr"));
        //创建一个文本请求对象
        BulkRequest bulkRequest = new BulkRequest();
        for (Item item : list) {
            bulkRequest.add(new IndexRequest("leyou", "item", item.getId().toString()).source(gson.toJson(item), XContentType.JSON));
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    @Test
    public void testQuery() throws IOException {
        //构建一个大范围的查询,可以支持各种查询方式,且指定索引库
        SearchRequest searchRequest = new SearchRequest("leyou");
        //用来构建查询方式
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //构建查询方式
        // searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        // searchSourceBuilder.fetchSource(new String[]{"id","title"},null);

        //  searchSourceBuilder.query(QueryBuilders.matchQuery("title", "小米手机"));
        //  searchSourceBuilder.postFilter(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("brand","小米"))
        //        .must(QueryBuilders.rangeQuery("price").gte(1000).lte(3000)));
     /*   searchSourceBuilder.from(0);
        searchSourceBuilder.size(2);
        searchSourceBuilder.sort("price",SortOrder.DESC);*/
        /*searchSourceBuilder.query(QueryBuilders.termQuery("title", "小米"));
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        highlightBuilder.field("title");
        searchSourceBuilder.highlighter(highlightBuilder);*/
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("brand_aggs").field("brand");
        searchSourceBuilder.aggregation(termsAggregationBuilder);
        //把查询方式放入searchRequest中
        searchRequest.source(searchSourceBuilder);

        //执行查询 返回searchresponse



        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Aggregations aggregations = searchResponse.getAggregations();
        Terms terms = aggregations.get("brand_aggs");
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            System.out.println(bucket.getKeyAsString()+":"+bucket.getDocCount());
        }

        SearchHits searchHits = searchResponse.getHits();
        System.out.println("查询的总条数" + searchHits.getTotalHits());
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            Item item = gson.fromJson(sourceAsString, Item.class);
            //System.out.println(item);

          /*  Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            Text[] fragments = highlightField.getFragments();
            if (fragments != null && fragments.length > 0) {
                String title_highlight = fragments[0].toString();
                item.setTitle(title_highlight);
            }*/
            System.out.println(item);
        }
    }

    @After
    public void end() throws Exception {
        client.close();
    }
}
