package com.github.ipantazi.carsharing.mapper;

import com.github.ipantazi.carsharing.config.MapperConfig;
import com.github.ipantazi.carsharing.dto.user.UserProfileUpdateDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toUserEntity(UserRegistrationRequestDto requestDto);

    UserResponseDto toUserDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateUserEntity(UserProfileUpdateDto dto, @MappingTarget User user);
}
