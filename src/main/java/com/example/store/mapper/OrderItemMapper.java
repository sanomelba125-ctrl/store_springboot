package com.example.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.store.entity.OrderItem;
import com.example.store.vo.OrderItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 根据订单ID查询订单项列表，同时关联查询商品表获取名称和图片
     */
    @Select("SELECT oi.*, g.name as goodsName, g.pic as goodsPic " +
            "FROM order_item oi " +
            "LEFT JOIN goods g ON oi.goods_id = g.id " +
            "WHERE oi.order_id = #{orderId}")
    List<OrderItemVO> getItemsByOrderId(@Param("orderId") String orderId);
}