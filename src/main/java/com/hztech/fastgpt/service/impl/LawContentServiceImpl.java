package com.hztech.fastgpt.service.impl;

import cn.org.atool.fluent.mybatis.If;
import com.hztech.fastgpt.dao.ILawContentDao;
import com.hztech.fastgpt.dao.po.LawContentDO;
import com.hztech.fastgpt.dao.wrapper.LawContentQuery;
import com.hztech.fastgpt.mapper.wrapper.LawContentMapperWrapper;
import com.hztech.fastgpt.model.LawInfo;
import com.hztech.fastgpt.service.ILawContentService;
import com.hztech.service.transactionscript.impl.HzBaseTransactionScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ILawContentService 服务实现
 *
 * @author HZ
 */
@Service
@RequiredArgsConstructor
public class LawContentServiceImpl extends HzBaseTransactionScriptService<ILawContentDao, LawContentDO, Long>
        implements ILawContentService {

    private final LawContentMapperWrapper mapper;

    @Override
    public List<LawInfo> getAll(LocalDateTime dateTime, Long minId, Long maxId) {
        LawContentQuery query = LawContentQuery.query()
                .select.id().outerId().type().status().title().subject().effective().publish().content().docFileUrl().pdfFileUrl().part().chapter().section().article().dataSource().end()
                .where.createTime().gt(dateTime, If::notNull).id().le(maxId, If::notNull).id().ge(minId, If::notNull).end();
        return mapper.findListByQuery(query, LawInfo.class);
    }
}
