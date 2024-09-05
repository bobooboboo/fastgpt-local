package com.hztech.fastgpt.dao.impl;

import com.hztech.dao.base.HzFmBaseDao;
import com.hztech.fastgpt.dao.IArticleDao;
import com.hztech.fastgpt.dao.mapper.ArticleMapper;
import com.hztech.fastgpt.dao.po.ArticleDO;
import com.hztech.fastgpt.dao.wrapper.ArticleQuery;
import com.hztech.fastgpt.dao.wrapper.ArticleUpdate;
import com.hztech.fastgpt.mapper.wrapper.ArticleMapperWrapper;
import org.springframework.stereotype.Repository;

/**
 * ArticleDaoImpl
 *
 * @author HZ
 */
@Repository
public class ArticleDaoImpl extends HzFmBaseDao<ArticleMapperWrapper, ArticleMapper, ArticleQuery, ArticleUpdate, ArticleDO, Long>
        implements IArticleDao {

}