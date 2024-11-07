package com.hztech.fastgpt.service;

import com.hztech.fastgpt.dao.po.HigherLevelLawDO;
import com.hztech.service.transactionscript.IHzTransactionScriptService;

import java.util.List;

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

    /**
     * 获取所有上位法的标题
     */
    List<String> getHigherLevelLawTitle();
}
