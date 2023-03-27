package cn.tedu.mall.search.service.impl;


import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.model.Spu;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {
    @DubboReference
    private IForFrontSpuService dubboSpuService;
    //创建可以新增到es的持久层
    @Autowired
    private SpuForElasticRepository spuRepository;

    @Override
    public JsonPage<SpuForElastic> search(String keyword, Integer page, Integer pageSize) {
        //根据执行中的方法参数 spring data框架分页页码从0开始
        Page<SpuForElastic> spus = spuRepository.querySearch(keyword, PageRequest.of(page-1,pageSize));
        //当前方法业务逻辑层方法要求返回jsonpage 但查询结果为page类型 需要做转换
        JsonPage<SpuForElastic> jsonPage = new JsonPage<>();
        //赋值分页信息
        jsonPage.setPage(page);
        jsonPage.setPageSize(pageSize);
        //赋值总页数和总条数
        jsonPage.setTotalPage(spus.getNumberOfElements());
        jsonPage.setTotalPage(spus.getTotalPages());
        //赋值分页数据
        jsonPage.setList(spus.getContent());
        //最后返回
        return jsonPage;
    }

    //dubbo调用product模块查询spu
    @Override
    public void loadSpuByPage() {
        //循环完成分页查询所有数据
        //查出一页信息就新增到es中 直到最后一页
        int i = 1; // 循环次数变量i 同时代表页码
        int pages = 0; //总页数 在第一次循环运行后才能知道具体值 默认赋值0 或不赋值
        do {
            // dubbo调用查询当前页的spu数据
            JsonPage<Spu> spus = dubboSpuService.getSpuByPage(i,2);
            //需要将jsonpage类型中的数据转换为list
            List<SpuForElastic> esSpus = new ArrayList<>();
            //遍历spus 进行转换 并新增到esspus
            for  ( Spu spu :spus.getList() ) {
                SpuForElastic esSpu = new SpuForElastic();
                BeanUtils.copyProperties(spu,esSpu);
                //将转换完成的对象添加到集合中
                esSpus.add(esSpu);
            }
            //esSpu集合中已经包含了每页数据 可以执行批量新增到es中
            spuRepository.saveAll(esSpus);
            i++;
            pages = spus.getTotalPage();
        }while (i <= pages);

    }
}
