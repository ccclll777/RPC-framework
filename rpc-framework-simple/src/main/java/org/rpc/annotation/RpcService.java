package org.rpc.annotation;


import java.lang.annotation.*;

/**
 * RPC service annotation, marked on the service implementation class
 *
 * @author ccclll777
 * @createTime 2022年12月12日 13:11:00
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface RpcService {

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";

}
