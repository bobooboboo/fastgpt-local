package com.hztech.fastgpt.dao.impl;

import com.hztech.dao.base.HzFmBaseDao;
import com.hztech.fastgpt.dao.ILawContentBakDao;
import com.hztech.fastgpt.dao.mapper.LawContentBakMapper;
import com.hztech.fastgpt.dao.po.LawContentBakDO;
import com.hztech.fastgpt.dao.wrapper.LawContentBakQuery;
import com.hztech.fastgpt.dao.wrapper.LawContentBakUpdate;
import com.hztech.fastgpt.mapper.wrapper.LawContentBakMapperWrapper;
import org.springframework.stereotype.Repository;

/**
 * LawContentBakDaoImpl
 *
 * @author HZ
 */
@Repository
public class LawContentBakDaoImpl extends HzFmBaseDao<LawContentBakMapperWrapper, LawContentBakMapper, LawContentBakQuery, LawContentBakUpdate, LawContentBakDO, Long>
        implements ILawContentBakDao {

}