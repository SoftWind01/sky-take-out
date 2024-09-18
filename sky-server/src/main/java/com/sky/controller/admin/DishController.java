package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@Api(tags = "菜品相关接口")
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation("增加菜品")
    @PostMapping
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("增加菜品");
        dishService.saveWithFlavor(dishDTO);
        String key="dish_"+dishDTO.getCategoryId();
        cleanCache(key);
        return Result.success();
    }

    @ApiOperation("菜品分类查询")
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分类查询:{}",dishPageQueryDTO);
        PageResult pageResult=dishService.page(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @ApiOperation("菜品批量删除")
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除：{}",ids);
        dishService.delete(ids);
        cleanCache("dish_*");
        return Result.success();
    }

    @ApiOperation("根据id查询菜品")
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品：{}",id);
        DishVO dishVO= dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @ApiOperation("修改菜品")
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}",dishDTO);
        dishService.update(dishDTO);

        cleanCache("dish_*");
        return Result.success();
    }

    @ApiOperation("菜品起售/停售")
    @PostMapping("/status/{status}")
    public Result updateStatus(@PathVariable Integer status,Long id){
        log.info("菜品起售/停售：{},{}",status,id);
        dishService.updateStats(status,id);

        cleanCache("dish_*");
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<DishVO>> list(String categoryId){
        List<DishVO> dishVOList=dishService.getByCategoryId(categoryId);
        return Result.success(dishVOList);
    }

    /**
     * 清理缓存
     * @param pattern
     */
    private  void cleanCache(String pattern){
        Set keys=redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}
