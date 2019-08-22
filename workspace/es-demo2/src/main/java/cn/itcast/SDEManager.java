package cn.itcast;

import cn.itcast.repository.GoodsRepository;
import com.google.gson.Gson;
import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import pojo.Goods;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @创建人 xgh
 * @创建时间 2019/8/2022:36
 * @描述
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SDEManager {
    @Autowired
    private ElasticsearchTemplate esTemplate;
    @Autowired
    private GoodsRepository goodsRepository;
    @Test
    public void createIndex(){
    //esTemplate.createIndex(Goods.class);
    esTemplate.putMapping(Goods.class);
    }
    @Test
    public void testDoc(){
        ArrayList<Goods> list = new ArrayList<Goods>();
        list.add(new Goods(1L, "小米6X手机", "手机", "小米", 1299.0, "xiaomi6s"));
        list.add(new Goods(2L, "华为p30手机", "手机", "华为", 3299.0, "sdfhe"));
        list.add(new Goods(3L, "apple5se手机", "手机", "苹果", 2299.0, "qrgjry"));
        list.add(new Goods(4L, "小米7X手机", "手机", "小米", 2888.0, "eiert4"));
        list.add(new Goods(5L, "荣耀V10", "手机", "荣耀", 1799.0, "kgfgr"));
        goodsRepository.saveAll(list);
    }
    @Test
    public void testQuery(){
     /*   Iterable<Goods> goodslist = goodsRepository.findAll();
        for (Goods goods : goodslist) {
            System.out.println(goods);
        }*/

    /*    Page<Goods> goodlist = goodsRepository.findAll(PageRequest.of(0, 4));
        for (Goods goods : goodlist) {
            System.out.println(goods);
        }*/
       // List<Goods> goods = goodsRepository.findByTitle("小米");
        List<Goods> goods = goodsRepository.findByPriceBetween(2000.0,3000.0);
        for (Goods good : goods) {
            System.out.println(good);
        }
    }

    @Test
    public void nativeQuery(){
       NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withQuery(QueryBuilders.termQuery("title","小米"));

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        highlightBuilder.field("title");
       nativeSearchQueryBuilder.withHighlightBuilder(highlightBuilder);
        nativeSearchQueryBuilder.withHighlightFields(new HighlightBuilder.Field("title"));
        AggregatedPage<Goods> aggregatedPage = esTemplate.queryForPage(nativeSearchQueryBuilder.build(), Goods.class, new SearchResultMapperImpl<Goods>());
        List<Goods> goodsList = aggregatedPage.getContent();
        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }
    Gson gson = new Gson();
    class SearchResultMapperImpl<T> implements SearchResultMapper{
        @Override
        public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
            long total = searchResponse.getHits().getTotalHits();
            Aggregations aggregations = searchResponse.getAggregations();
            String scrollId = searchResponse.getScrollId();
            float maxScore = searchResponse.getHits().getMaxScore();
            List<T> content = new ArrayList<T>();
            SearchHit[] hits = searchResponse.getHits().getHits();
            for (SearchHit hit : hits) {
                String sourceAsString = hit.getSourceAsString();
                T t = gson.fromJson(sourceAsString, aClass);

                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("title");
                Text[] fragments = highlightField.getFragments();
                if (fragments != null && fragments.length > 0) {
                    String title_highlight = fragments[0].toString();
                    try {
                        BeanUtils.setProperty(t,"title",title_highlight);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                content.add(t);
            }
            return new AggregatedPageImpl<T>(content,pageable,total,aggregations,scrollId,maxScore);
        }
    }
}
