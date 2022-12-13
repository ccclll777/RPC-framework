package org.rpc.extension;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)//这种类型的Annotations将被JVM保留,所以他们能在运行时被JVM或其他使用反射机制的代码所读取和使用.
@Target(ElementType.TYPE)//说明了Annotation所修饰的对象范围 这个类只能用于描述类、接口(包括注解类型) 或enum声明
public @interface SPI {
}
