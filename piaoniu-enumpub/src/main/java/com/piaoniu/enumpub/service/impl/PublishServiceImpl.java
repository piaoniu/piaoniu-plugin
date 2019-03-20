package com.piaoniu.enumpub.service.impl;

import com.google.common.collect.Lists;
import com.piaoniu.enumpub.domain.TypeValue;
import com.piaoniu.enumpub.service.PublishManager;
import com.piaoniu.enumpub.service.PublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PublishServiceImpl implements PublishService {

    @Autowired
    private PublishManager publishManager;

    @Override
    public List<String> publishEnumNames() {
        return publishManager.getSimpleNames();
    }

    @Override
    public List<TypeValue> publishEnum(String enumName) {
        try{
            Class enumClass = getEnumClass(enumName);
            Object[] objects = enumClass.getEnumConstants();
            Method getValue = enumClass.getMethod("getValue");
            Method getDesc = enumClass.getMethod("getDesc");
            return Lists.newArrayList(objects).stream().map(o -> {
                    try{
                        return TypeValue.of(Integer.valueOf(getValue.invoke(o).toString()),getDesc.invoke(o).toString());
                    }catch (Exception e){
                        return null;
                    }
                }
            ).collect(Collectors.toList());
        } catch (Exception e){
            return Lists.newArrayList();
        }
    }

    private Class getEnumClass(String name) throws ClassNotFoundException{
        String realName = publishManager.getRealName(name);
        return Class.forName(realName);
    }
}
