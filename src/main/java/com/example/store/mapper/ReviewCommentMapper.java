package com.example.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.store.entity.ReviewComment;
import com.example.store.vo.ReviewCommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewCommentMapper extends BaseMapper<ReviewComment> {

    @Select("SELECT c.*, u.nickname, tu.nickname as target_nickname " +
            "FROM review_comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "LEFT JOIN user tu ON c.target_user_id = tu.id " +
            "WHERE c.review_id = #{reviewId} " +
            "ORDER BY c.create_time ASC")
    List<ReviewCommentVO> getCommentsByReviewId(Page<ReviewCommentVO> page, @Param("reviewId") String reviewId);
}
