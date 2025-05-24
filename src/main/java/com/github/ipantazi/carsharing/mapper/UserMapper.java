package com.github.ipantazi.carsharing.mapper;

import com.github.ipantazi.carsharing.config.MapperConfig;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationResponseDto;
import com.github.ipantazi.carsharing.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toUserEntity(UserRegistrationRequestDto requestDto);

    UserRegistrationResponseDto toUserDto(User user);
}
