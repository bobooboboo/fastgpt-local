package com.hztech.fastgpt.service;

import com.hztech.fastgpt.dao.po.HigherLevelLawDO;
import com.hztech.service.transactionscript.IHzTransactionScriptService;

/**
 * IHigherLevelLawService 服务接口
 *
 * @author HZ
 */
public interface IHigherLevelLawService extends IHzTransactionScriptService<HigherLevelLawDO, Long> {

    /**
     * 查询法律法规上位法
     *
     * @param title 法律法规标题
     */
    String getHigherLevelLaw(String title);

    void initHigherLevelLaw();
}
