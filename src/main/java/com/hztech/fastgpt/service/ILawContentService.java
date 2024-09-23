package com.hztech.fastgpt.service;

import com.hztech.fastgpt.dao.po.LawContentDO;
import com.hztech.fastgpt.model.LawInfo;
import com.hztech.service.transactionscript.IHzTransactionScriptService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ILawContentService 服务接口
 *
 * @author HZ
 */
public interface ILawContentService extends IHzTransactionScriptService<LawContentDO, Long> {
    List<LawInfo> getAll(LocalDateTime dateTime, Long minId, Long maxId);
}
