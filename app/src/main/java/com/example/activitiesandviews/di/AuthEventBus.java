package com.example.activitiesandviews.di;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthEventBus {

    private final MutableLiveData<Boolean> sessionExpired = new MutableLiveData<>();

    @Inject
    public AuthEventBus() {}

    public LiveData<Boolean> getSessionExpired() {
        return sessionExpired;
    }

    public void emitSessionExpired() {
        sessionExpired.postValue(true); // postValue es thread-safe (funciona desde hilo de red)
    }
}
