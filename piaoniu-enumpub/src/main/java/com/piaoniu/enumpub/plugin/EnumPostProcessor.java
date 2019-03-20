package com.piaoniu.enumpub.plugin;

import com.piaoniu.enumpub.annotations.EnumPub;
import com.piaoniu.enumpub.service.PublishManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

@Component
public class EnumPostProcessor implements BeanPostProcessor {

    @Autowired
    private PublishManager publishManager;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
        if (methods != null) {
            for (Method method : methods) {
                if (null != AnnotationUtils.findAnnotation(method, EnumPub.class)) {
                    String name = bean.getClass().getName();
                    String simpleName = bean.getClass().getSimpleName();
                    publishManager.saveNames(name,simpleName);
                }
            }
        }
        return bean;
    }
}
