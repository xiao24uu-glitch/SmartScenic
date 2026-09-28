package com.scenic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scenic.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    @Update("UPDATE coupon SET received_count = received_count + 1 WHERE id = #{id} AND received_count < total_count")
    int increaseReceivedCount(Long id);
}
