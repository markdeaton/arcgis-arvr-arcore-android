package com.esri.arcgisruntime.arvrdemos.arvrinthearcgisruntimesdkforandroid;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;

public interface SceneUpdateCallable {
    void onSceneUpdate(Scene scene, FrameTime frameTime);
}
