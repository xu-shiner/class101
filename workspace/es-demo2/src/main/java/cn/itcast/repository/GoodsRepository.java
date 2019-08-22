package cn.itcast.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;
import pojo.Goods;

import java.util.List;

/**
 * @创建人 xgh
 * @创建时间 2019/8/2118:03
 * @描述
 */
public interface GoodsRepository extends ElasticsearchCrudRepository<Goods,Long> {
    List<Goods> findByTitle(String 小米);

    List<Goods> findByPriceBetween(double v, double v1);
}
