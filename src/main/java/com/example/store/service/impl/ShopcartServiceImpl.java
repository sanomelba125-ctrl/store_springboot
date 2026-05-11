package com.example.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.store.entity.Goods;
import com.example.store.entity.Shopcart;
import com.example.store.mapper.GoodsMapper;
import com.example.store.mapper.ShopcartMapper;
import com.example.store.service.ShopcartService;
import com.example.store.utils.DateUtil;
import com.example.store.utils.Result;
import com.example.store.utils.UrlHelper;
import com.example.store.vo.ShopcartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ShopcartServiceImpl extends ServiceImpl<ShopcartMapper, Shopcart> implements ShopcartService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private UrlHelper urlHelper;

    @Override
    public Result addCart(String userId, String goodsId, Integer number) {
        // 检查商品是否存在
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == 0) {
            return new Result().fail("商品已下架或不存在");
        }
        if (goods.getInventory() < number) {
            return new Result().fail("库存不足");
        }

        //  查询该用户是否已经加购过该商品
        QueryWrapper<Shopcart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("goods_id", goodsId);
        Shopcart existCart = this.getOne(wrapper);

        if (existCart != null) {
            // 存在就追加数量
            existCart.setNumber(existCart.getNumber() + number);
            this.updateById(existCart);
        } else {
            //不存在就新增记录
            Shopcart shopcart = new Shopcart();
            shopcart.setUserId(userId);
            shopcart.setGoodsId(goodsId);
            shopcart.setNumber(number);
            shopcart.setAddPrice(goods.getPrice()); // 记录加入时的价格
            shopcart.setCreateTime(DateUtil.getCurrentTime());
            this.save(shopcart);
        }

        return new Result().success("已加入购物车");
    }

    @Override
    public Result getMyCart(String userId, String keyword) {
        List<ShopcartVO> list = baseMapper.getCartListByUserId(userId, keyword);

        for (ShopcartVO vo : list) {
            vo.setGoodsPic(urlHelper.enrichUrl(vo.getGoodsPic()));
        }
        return new Result().success().setData(list);
    }

    @Override
    public Result updateNumber(String id, Integer number) {
        if (number < 1) {
            return new Result().fail("数量不能少于1");
        }
        Shopcart shopcart = this.getById(id);
        if (shopcart != null) {
            shopcart.setNumber(number);
            this.updateById(shopcart);
        }
        return new Result().success();
    }

    @Override
    public Result deleteCart(String ids) {
        if (ids != null && !ids.isEmpty()) {
            List<String> idList = Arrays.asList(ids.split(","));
            this.removeByIds(idList);
        }
        return new Result().success("删除成功");
    }
}