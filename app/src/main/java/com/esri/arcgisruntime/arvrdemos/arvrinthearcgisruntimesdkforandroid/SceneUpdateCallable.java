package com.esri.arcgisruntime.arvrdemos.arvrinthearcgisruntimesdkforandroid;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;

public interface SceneUpdateCallable {
    void onSceneUpdate(Scene scene, Session session, Frame frame, FrameTime frameTime);
    void onSceneError(Exception e);
}
