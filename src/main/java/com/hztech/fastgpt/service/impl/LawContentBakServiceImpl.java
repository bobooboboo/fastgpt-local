package com.hztech.fastgpt.service.impl;

import com.hztech.fastgpt.dao.ILawContentBakDao;
import com.hztech.fastgpt.dao.po.LawContentBakDO;
import com.hztech.fastgpt.service.ILawContentBakService;
import com.hztech.service.transactionscript.impl.HzBaseTransactionScriptService;
import org.springframework.stereotype.Service;

/**
 * ILawContentBakService 服务实现
 *
 * @author HZ
 */
@Service
public class LawContentBakServiceImpl extends HzBaseTransactionScriptService<ILawContentBakDao, LawContentBakDO, Long>
        implements ILawContentBakService {

}
