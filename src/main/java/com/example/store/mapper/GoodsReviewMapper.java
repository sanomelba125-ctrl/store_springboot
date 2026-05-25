package com.example.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.store.entity.GoodsReview;
import com.example.store.vo.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface GoodsReviewMapper extends BaseMapper<GoodsReview> {

    // 按商品 ID 分页查评价，支持只看有图过滤
    @Select("<script>" +
            "SELECT r.*, u.nickname " +
            "FROM goods_review r LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.goods_id = #{goodsId} " +
            "<if test='hasImages != null and hasImages == true'> AND r.images IS NOT NULL AND r.images != '' </if> " +
            "ORDER BY r.create_time DESC" +
            "</script>")
    List<ReviewVO> getReviewsByGoodsId(Page<ReviewVO> page, @Param("goodsId") String goodsId, @Param("hasImages") Boolean hasImages);

    // 检查某订单项是否已评价
    @Select("SELECT COUNT(*) FROM goods_review WHERE order_item_id = #{orderItemId}")
    int countByOrderItemId(@Param("orderItemId") String orderItemId);

    // 聚合查询：某商品的平均分和总评价数
    @Select("SELECT COALESCE(AVG(rating), 0) as avgRating, COUNT(*) as totalCount " +
            "FROM goods_review WHERE goods_id = #{goodsId}")
    Map<String, Object> getReviewStats(@Param("goodsId") String goodsId);
}
