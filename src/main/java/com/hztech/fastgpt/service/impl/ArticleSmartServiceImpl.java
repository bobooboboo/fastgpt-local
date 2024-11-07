package com.hztech.fastgpt.service.impl;

import com.hztech.fastgpt.dao.po.ArticleSmartDO;
import com.hztech.fastgpt.service.IArticleSmartService;
import com.hztech.service.transactionscript.impl.HzBaseTransactionScriptService;
import com.hztech.fastgpt.dao.IArticleSmartDao;
import org.springframework.stereotype.Service;

/**
 * IArticleSmartService 服务实现
 *
 * @author HZ
 */
@Service
public class ArticleSmartServiceImpl extends HzBaseTransactionScriptService<IArticleSmartDao, ArticleSmartDO, Long>
        implements IArticleSmartService {

}
