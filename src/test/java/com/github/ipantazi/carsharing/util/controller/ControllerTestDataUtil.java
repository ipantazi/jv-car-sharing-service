package com.github.ipantazi.carsharing.util.controller;

import com.github.ipantazi.carsharing.util.TestDataUtil;
import java.util.List;
import org.springframework.http.HttpStatus;

public class ControllerTestDataUtil extends TestDataUtil {
    public static final int NO_CONTENT = HttpStatus.NO_CONTENT.value();
    public static final int BAD_REQUEST = HttpStatus.BAD_REQUEST.value();
    public static final int NOT_FOUND = HttpStatus.NOT_FOUND.value();
    public static final int UNPROCESSABLE_ENTITY = HttpStatus.UNPROCESSABLE_ENTITY.value();
    public static final int UNAUTHORIZED = HttpStatus.UNAUTHORIZED.value();
    public static final int FORBIDDEN = HttpStatus.FORBIDDEN.value();
    public static final int CONFLICT = HttpStatus.CONFLICT.value();

    public static final String URL_CARS = "/cars";
    public static final String URL_CARS_EXISTING_CAR_ID = "/cars/" + EXISTING_CAR_ID;
    public static final String URL_CARS_NOT_EXISTING_CAR_ID = "/cars/" + NOT_EXISTING_CAR_ID;
    public static final String URL_CARS_SAFE_DELETED_CAR_ID = "/cars/" + SAFE_DELETED_CAR_ID;

    public static final String URL_LOGIN = "/auth/login";
    public static final String URL_REGISTRATION = "/auth/registration";

    public static final List<String> EXPECTED_SAVE_CAR_ERRORS = List.of(
            "Field 'brand': Invalid brand. Brand must be between 3 and 50 characters.",
            "Field 'inventory': Invalid inventory. Inventory should be positive.",
            "Field 'type': Invalid type. Type must be between 3 and 20 characters.",
            "Field 'model': Invalid model. Model must be between 1 and 50 characters.",
            "Field 'dailyFee': Invalid daily fee. The maximum allowed number for a daily fee is "
                    + "10 digits and 2 digits after the decimal point."
    );
    public static final List<String> EXPECTED_UPDATE_CAR_ERRORS = List.of(
            "Field 'brand': Invalid brand. Brand must be between 3 and 50 characters.",
            "Field 'type': Invalid type. Type must be between 3 and 20 characters.",
            "Field 'model': Invalid model. Model must be between 1 and 50 characters.",
            "Field 'dailyFee': Invalid daily fee. The maximum allowed number for a daily fee is "
                    + "10 digits and 2 digits after the decimal point."
    );
    public static final List<String> EXPECTED_SAVE_CAR_NULL_ERRORS = List.of(
            "Field 'brand': Invalid brand. Brand can't be blank.",
            "Field 'inventory': Invalid inventory. Inventory should be positive.",
            "Field 'type': Invalid type. Type can't be blank.",
            "Field 'model': Invalid model. Model can't be blank.",
            "Field 'dailyFee': Invalid daily fee. Daily fee shouldn't be null."
    );
    public static final List<String> EXPECTED_UPDATE_CAR_NULL_ERRORS = List.of(
            "Field 'brand': Invalid brand. Brand can't be blank.",
            "Field 'type': Invalid type. Type can't be blank.",
            "Field 'model': Invalid model. Model can't be blank.",
            "Field 'dailyFee': Invalid daily fee. Daily fee shouldn't be null."
    );
    public static final List<String> EXPECTED_UPDATE_INVENTORY_CAR_ERRORS = List.of(
            "Field 'inventory': Invalid inventory. Inventory should be positive.",
            "Field 'operation': Invalid operation. Operation shouldn't be null."
    );
    public static final List<String> EXPECTED_USER_LOGIN_BLANK_ERRORS = List.of(
            "Field 'email': Invalid email. Email shouldn't be blank.",
            "Field 'password': Invalid password. Password shouldn't be blank.",
            "Field 'password': Invalid password. The password should be between 8 to 50."
    );
    public static final List<String> EXPECTED_USER_LOGIN_ERRORS = List.of(
            "Field 'email': Invalid format email.",
            "Field 'email': Email address should be exceed 50 characters.",
            "Field 'password': Invalid password. The password should be between 8 to 50."
    );
    public static final List<String> EXPECTED_USER_REGISTRATION_SIZE_ERRORS = List.of(
            "Field 'email': Invalid format email.",
            "Field 'email': Email address must not exceed 50 characters.",
            "Field 'password': Invalid password. Password shouldn't be blank.",
            "Field 'password': Invalid password. The password should be between 8 to 50.",
            "Field 'password': Password must include at least one lowercase letter, "
                    + "one uppercase letter, one number, and one special character.",
            "Field 'firstName': Invalid first name. First name shouldn't be blank.",
            "Field 'firstName': Invalid first name. First name should be between 3 to 50.",
            "Field 'lastName': Invalid last name. Last name shouldn't be blank.",
            "Field 'lastName': Invalid last name. Last name should be between 3 to 50."
    );
    public static final List<String> EXPECTED_USER_REGISTRATION_FORMAT_ERRORS = List.of(
            "Field 'email': Invalid format email.",
            "Field 'password': Password must include at least one lowercase letter, "
                    + "one uppercase letter, one number, and one special character.",
            "Field 'firstName': First name must contain only letters.",
            "Field 'lastName': Last name must be contain only letters."
    );

    private ControllerTestDataUtil() {
    }
}
