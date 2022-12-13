package org.rpc.config;


import lombok.*;


@AllArgsConstructor//自动生成所有参数的构造方法
@NoArgsConstructor//自动生成无参数构造方法
@Getter //Getter方法
@Setter //Setter方法
@Builder
@ToString
public class RpcServiceConfig {
    /**
     * 服务版本
     */
    private String version = "";
    /**
     * 当接口有多个实现类时，按组区分
     */
    private String group = "";

    /**
     * target service
     */
    private Object service;

    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }
}
