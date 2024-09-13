package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    List<Long> getSetmealIdsByDishids(List<Long> dishIds);

    void insert(SetmealDish setmealDish);

    @Delete("delete from setmeal_dish where setmeal_id=#{id}")
    void deleteBySetmealId(Long id);

    @Select("select * from setmeal_dish where dish_id=#{id}")
    List<SetmealDish> getASetmealIdsByDishids(Long id);
}
