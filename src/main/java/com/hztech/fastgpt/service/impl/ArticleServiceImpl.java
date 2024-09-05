package com.hztech.fastgpt.service.impl;

import com.hztech.fastgpt.dao.po.ArticleDO;
import com.hztech.fastgpt.service.IArticleService;
import com.hztech.service.transactionscript.impl.HzBaseTransactionScriptService;
import com.hztech.fastgpt.dao.IArticleDao;
import org.springframework.stereotype.Service;

/**
 * IArticleService 服务实现
 *
 * @author HZ
 */
@Service
public class ArticleServiceImpl extends HzBaseTransactionScriptService<IArticleDao, ArticleDO, Long>
        implements IArticleService {

}
