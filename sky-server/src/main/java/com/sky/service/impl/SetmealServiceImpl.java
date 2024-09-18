package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    @Override
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        for(SetmealDish setmealDish : setmealDishList) {
            setmealDish.setSetmealId(setmeal.getId());
            setmealDishMapper.insert(setmealDish);
        }
    }

    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<Setmeal> page=setmealMapper.getSetmeal(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);
        setmealDishMapper.deleteBySetmealId(setmeal.getId());
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        if(setmealDishList!=null&& !setmealDishList.isEmpty()) {
            for(SetmealDish setmealDish : setmealDishList) {
                setmealDish.setSetmealId(setmeal.getId());
                setmealDishMapper.insert(setmealDish);
            }
        }
    }

    @Override
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.getSetmealById(id);
        List<SetmealDish> setmealDishList=setmealDishMapper.getASetmealIdsByDishids(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishList);
        return setmealVO;
    }

    @Override
    public void delete(List<Long> ids) {
        for(Long id : ids) {
            Setmeal setmeal = setmealMapper.getSetmealById(id);
            if(setmeal==null||setmeal.getStatus()==StatusConstant.ENABLE) {
                throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        setmealMapper.delete(ids);
        for(Long id : ids) {
            setmealDishMapper.deleteBySetmealId(id);
        }
    }

    @Override
    public void updateStatus(Integer status, Long id) {
        if(status== StatusConstant.ENABLE){
            List<Dish> dishList=dishMapper.getBySetmealId(id);
            if(dishList!=null&&dishList.size()>0) {
                for(Dish dish : dishList) {
                    if(dish.getStatus()==StatusConstant.DISABLE){
                        throw new SetmealEnableFailedException(MessageConstant.DISH_DISABLE);
                    }
                }
            }
        }
        /*Setmeal setmeal = setmealMapper.getSetmealById(id);
        setmeal.setStatus(status);*/
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }


}
