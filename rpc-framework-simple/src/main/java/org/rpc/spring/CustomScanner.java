package org.rpc.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

/*
 * @author shuang.kou
 * @createTime 2022年12月12日 13:11:00
 */
public class CustomScanner  extends ClassPathBeanDefinitionScanner {
    /*
    ClassPathBeanDefinitionScanner作用就是将指定包下的类通过一定规则过滤后， 将Class 信息包装成 BeanDefinition 的形式，注册到IOC容器中。
     */
    public CustomScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annoType) {
        super(registry);
        super.addIncludeFilter(new AnnotationTypeFilter(annoType));
    }
    @Override
    public int scan(String... basePackages) {
        return super.scan(basePackages);
    }
}
