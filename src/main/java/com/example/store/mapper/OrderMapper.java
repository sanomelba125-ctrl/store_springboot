package com.example.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.store.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Select("<script>" +
            "SELECT DISTINCT o.* FROM `order` o " +
            "LEFT JOIN order_item oi ON o.id = oi.order_id " +
            "LEFT JOIN goods g ON oi.goods_id = g.id " +
            "LEFT JOIN shop s ON g.shop_id = s.id " +
            "WHERE s.user_id = #{adminId} " +
            "<if test='status != null'> AND o.status = #{status} </if> " +
            "ORDER BY o.create_time DESC" +
            "</script>")
    Page<Order> selectOrdersByAdminId(Page<Order> page,
                                      @Param("adminId") String adminId,
                                      @Param("status") Integer status);
}