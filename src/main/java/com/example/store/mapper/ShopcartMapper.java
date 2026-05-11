package com.example.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.store.entity.Shopcart;
import com.example.store.vo.ShopcartVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShopcartMapper extends BaseMapper<Shopcart> {

    // 根据用户ID查询购物车详情
    // 使用 <script> 标签支持动态 SQL
    // 增加了 keyword 判断：如果 keyword 不为空，就搜索 商品名 OR 店铺名
    @Select("<script>" +
            "SELECT c.*, " +
            "g.name as goodsName, g.pic as goodsPic, g.price as goodsPrice, g.inventory as goodsInventory, g.shop_id as shopId, " +
            "s.shopname as shopName " +
            "FROM shopcart c " +
            "LEFT JOIN goods g ON c.goods_id = g.id " +
            "LEFT JOIN shop s ON g.shop_id = s.id " +
            "WHERE c.user_id = #{userId} " +

            "<if test='keyword != null and keyword != \"\"'> " +
            "  AND (g.name LIKE CONCAT('%', #{keyword}, '%') OR s.shopname LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "ORDER BY c.create_time DESC" +
            "</script>")
    List<ShopcartVO> getCartListByUserId(@Param("userId") String userId, @Param("keyword") String keyword);
}