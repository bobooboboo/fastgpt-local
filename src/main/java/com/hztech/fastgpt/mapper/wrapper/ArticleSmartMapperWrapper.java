package com.hztech.fastgpt.mapper.wrapper;

import com.hztech.fastgpt.dao.mapper.ArticleSmartMapper;
import com.hztech.fastgpt.dao.po.ArticleSmartDO;
import com.hztech.fastgpt.dao.wrapper.ArticleSmartQuery;
import com.hztech.fastgpt.dao.wrapper.ArticleSmartUpdate;

import com.hztech.mapper.wrapper.BaseFmMapperWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * ArticleSmartMapper包装器
 *
 * @author HZ
 */
@Component
public class ArticleSmartMapperWrapper extends BaseFmMapperWrapper<ArticleSmartMapper, ArticleSmartQuery, ArticleSmartUpdate, ArticleSmartDO, Long> {

    public ArticleSmartMapperWrapper(@Qualifier("articleSmartMapper") ArticleSmartMapper mapper) {
        super(mapper);
    }
}