package com.hztech.fastgpt.dao.impl;

import com.hztech.dao.base.HzFmBaseDao;
import com.hztech.fastgpt.mapper.wrapper.ArticleSmartMapperWrapper;
import com.hztech.fastgpt.dao.IArticleSmartDao;
import com.hztech.fastgpt.dao.mapper.ArticleSmartMapper;
import com.hztech.fastgpt.dao.po.ArticleSmartDO;
import com.hztech.fastgpt.dao.wrapper.ArticleSmartQuery;
import com.hztech.fastgpt.dao.wrapper.ArticleSmartUpdate;
import org.springframework.stereotype.Repository;

/**
 * ArticleSmartDaoImpl
 *
 * @author HZ
 */
@Repository
public class ArticleSmartDaoImpl extends HzFmBaseDao<ArticleSmartMapperWrapper, ArticleSmartMapper, ArticleSmartQuery, ArticleSmartUpdate, ArticleSmartDO, Long>
        implements IArticleSmartDao {

}