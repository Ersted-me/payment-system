package com.ersted.service;

import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMetrics {

    private final Counter userRegisterSuccessful;
    private final Counter userRegisterFailed;

    private final Counter userLoginSuccessful;
    private final Counter userLoginFailed;

    private final Counter userGetInfoSuccessful;
    private final Counter userGetInfoFailed;

    private final Counter userRefreshTokenSuccessful;
    private final Counter userRefreshTokenFailed;


    public void recordGetInfoSuccess(){
        userGetInfoSuccessful.increment();
    }

    public void recordGetInfoFailure(){
        userGetInfoFailed.increment();
    }

    public void recordRegisterSuccess() {
        userRegisterSuccessful.increment();
    }

    public void recordRegisterFailure() {
        userRegisterFailed.increment();
    }

    public void recordLoginSuccess() {
        userLoginSuccessful.increment();
    }

    public void recordLoginFailure() {
        userLoginFailed.increment();
    }

    public void recordRefreshTokenSuccess() {
        userRefreshTokenSuccessful.increment();
    }

    public void recordRefreshTokenFailure() {
        userRefreshTokenFailed.increment();
    }

}
