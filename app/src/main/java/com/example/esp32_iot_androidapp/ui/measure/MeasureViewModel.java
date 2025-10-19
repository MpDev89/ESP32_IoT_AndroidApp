package com.example.esp32_iot_androidapp.ui.measure;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MeasureViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public MeasureViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}