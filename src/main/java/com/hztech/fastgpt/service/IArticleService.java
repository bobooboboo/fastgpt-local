package com.hztech.fastgpt.service;

import com.hztech.fastgpt.dao.po.ArticleDO;
import com.hztech.service.transactionscript.IHzTransactionScriptService;

/**
 * IArticleService 服务接口
 *
 * @author HZ
 */
public interface IArticleService extends IHzTransactionScriptService<ArticleDO, Long> {

}
