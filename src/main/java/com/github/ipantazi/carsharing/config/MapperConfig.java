package com.github.ipantazi.carsharing.config;

@org.mapstruct.MapperConfig(
        componentModel = "spring",
        injectionStrategy = org.mapstruct.InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = org.mapstruct.NullValueCheckStrategy.ALWAYS,
        implementationPackage = "<PACKAGE_NAME>.impl"
)
public class MapperConfig {
}
