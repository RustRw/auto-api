package org.duqiu.fly.autoapi.datasource.enums;

public enum DataSourceProtocol {
    JDBC("JDBC协议"),
    HTTP("HTTP协议"),
    NATIVE("原生协议"),
    TCP("TCP协议"),
    GRPC("gRPC协议");

    private final String displayName;

    DataSourceProtocol(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}