package com.scenic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scenic.entity.Tourist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TouristMapper extends BaseMapper<Tourist> {
}
