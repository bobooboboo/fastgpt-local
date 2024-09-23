package com.hztech.fastgpt.dao.impl;

import com.hztech.dao.base.HzFmBaseDao;
import com.hztech.fastgpt.dao.ILawContentDao;
import com.hztech.fastgpt.dao.mapper.LawContentMapper;
import com.hztech.fastgpt.dao.po.LawContentDO;
import com.hztech.fastgpt.dao.wrapper.LawContentQuery;
import com.hztech.fastgpt.dao.wrapper.LawContentUpdate;
import com.hztech.fastgpt.mapper.wrapper.LawContentMapperWrapper;
import org.springframework.stereotype.Repository;

/**
 * LawContentDaoImpl
 *
 * @author HZ
 */
@Repository
public class LawContentDaoImpl extends HzFmBaseDao<LawContentMapperWrapper, LawContentMapper, LawContentQuery, LawContentUpdate, LawContentDO, Long>
        implements ILawContentDao {

}