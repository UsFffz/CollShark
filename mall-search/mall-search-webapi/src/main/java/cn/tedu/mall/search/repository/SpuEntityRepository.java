package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuEntity;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpuEntityRepository extends ElasticsearchRepository<SpuEntity,Long> {
    //根据用户输入的关键字,查询ES中匹配的数据
    //参与查询的字段拼接成了一个字段 为Search_text 但实体类中并不存在该属性
    //所以只能编写查询语句 从es中搜索searchtext字段匹配的数据
    //由于老子用es不用这钟傻逼spring查询 不写了 只看思路就行 反正也不测试 都无所屌谓

}
