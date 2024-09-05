package com.hztech.fastgpt.mapper.wrapper;

import com.hztech.fastgpt.dao.mapper.ArticleMapper;
import com.hztech.fastgpt.dao.po.ArticleDO;
import com.hztech.fastgpt.dao.wrapper.ArticleQuery;
import com.hztech.fastgpt.dao.wrapper.ArticleUpdate;
import com.hztech.mapper.wrapper.BaseFmMapperWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * ArticleMapper包装器
 *
 * @author HZ
 */
@Component
public class ArticleMapperWrapper extends BaseFmMapperWrapper<ArticleMapper, ArticleQuery, ArticleUpdate, ArticleDO, Long> {

    public ArticleMapperWrapper(@Qualifier("articleMapper") ArticleMapper mapper) {
        super(mapper);
    }
}