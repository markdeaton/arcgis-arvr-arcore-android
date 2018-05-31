package com.esri.arcgisruntime.arvrdemos.arvrinthearcgisruntimesdkforandroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.esri.arcgisruntime.arvrdemos.arvrinthearcgisruntimesdkforandroid.util.ARUtils;
import com.esri.arcgisruntime.layers.ArcGISSceneLayer;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.FirstPersonCameraController;
import com.esri.arcgisruntime.mapping.view.PhoneMotionDataSource;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.mapping.view.SideBySideBarrelDistortionStereoRendering;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class MainActivity extends AppCompatActivity implements SceneUpdateCallable {

  private boolean isVR = false;

  private SceneView mSceneView;
  private ArSceneView mArSceneView;

  private TextView txtPose;

//  private android.hardware.Camera mCamera;
  private static final int PERMISSION_TO_USE_CAMERA = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mSceneView = findViewById(R.id.scene_view);

    if (isVR) {
      setUpVRScene();
    } else {
      // Request camera permissions...
      checkForCameraPermissions();
    }

    txtPose = findViewById(R.id.txtPose);
  }

  //Setup the Scene in Virtual Reality
  private void setUpVRScene() {
    mSceneView.setScene(new ArcGISScene(Basemap.Type.IMAGERY));
    // Add San Diego scene layer.  Example scene layers provided by Esri available here: http://www.arcgis.com/home/group.html?id=c4a19ab700fd4654b89a319b016eee03
    mSceneView.getScene().getOperationalLayers().add(new ArcGISSceneLayer(getString(R.string.url_scene_sd)));

    // Add elevation surface from ArcGIS Online
    mSceneView.getScene().getBaseSurface().getElevationSources().add(new ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"));

    // Define rendering mode for VR experience.
    mSceneView.setStereoRendering(new SideBySideBarrelDistortionStereoRendering());

    mSceneView.setAttributionTextVisible(false);

    completeVRSetup();
  }

  //Setup the Scene for Augmented Reality
  private void setUpARScene() {
    // Create scene without a basemap.  Background for scene content provided by device camera.
    mSceneView.setScene(new ArcGISScene());

    // Add San Diego scene layer.  This operational data will render on a video feed (eg from the device camera).
    mSceneView.getScene().getOperationalLayers().add(new ArcGISSceneLayer("https://tiles.arcgis.com/tiles/Imiq6naek6ZWdour/arcgis/rest/services/San_Diego_Textured_Buildings/SceneServer/layers/0"));

    // Enable AR for scene view.
    mSceneView.setARModeEnabled(true);

    // Create our Preview view and set it as the content of our activity.
    mArSceneView = new ArSceneView(this);

    // Create an instance of Camera
    FrameLayout preview = findViewById(R.id.camera_preview);
    preview.removeAllViews();
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    preview.addView(mArSceneView, params);

    Camera cameraSanDiego = new Camera(32.707, -117.157, 60, 180, 0, 0);
    FirstPersonCameraController fpcController = new FirstPersonCameraController();
    fpcController.setInitialPosition(cameraSanDiego);

    fpcController.setTranslationFactor(500);

    startArSession();

    ARCoreSource motionSource = new ARCoreSource(mArSceneView.getScene(), mArSceneView.getSession(), cameraSanDiego, this);
    fpcController.setDeviceMotionDataSource(motionSource);

    fpcController.setFramerate(FirstPersonCameraController.FirstPersonFramerate.BALANCED);
    mSceneView.setCameraController(fpcController);

    // To update position and orientation of the camera with device sensors use:
    motionSource.startAll();
  }

  private void completeVRSetup() {
    // Scene camera controlled by sensors
    Camera cameraSanDiego = new Camera(32.707, -117.157, 60, 180, 0, 0);
    FirstPersonCameraController fpcController = new FirstPersonCameraController();
    fpcController.setInitialPosition(cameraSanDiego);

//        fpcController.setTranslationFactor(100);

    // PhoneMotionDataSource works with both Android and iOS.
    PhoneMotionDataSource phoneSensors = new PhoneMotionDataSource(this);
    fpcController.setDeviceMotionDataSource(phoneSensors);

    fpcController.setFramerate(FirstPersonCameraController.FirstPersonFramerate.BALANCED);
    mSceneView.setCameraController(fpcController);
    // To update position and orientation of the camera with device sensors use:
    phoneSensors.startUpdatingAngles(false);
    // To update location of camera in the scene with device location (GPS) use:
    //phoneSensors.startUpdatingInitialPosition();
    // To update both use:
//        phoneSensors.startAll();
  }

  @Override
  protected void onDestroy() {
    if (mArSceneView != null) mArSceneView.destroy();
    super.onDestroy();
  }

  @Override
  protected void onPause(){
    mSceneView.pause();
    if (mArSceneView != null) mArSceneView.pause();
    super.onPause();
  }

  private boolean installRequested;
  private void startArSession() {
    if (mArSceneView != null) {
      if (mArSceneView.getSession() == null) {
        // If the session wasn't created yet, don't resume rendering.
        // This can happen if ARCore needs to be updated or permissions are not granted yet.
        try {
          Session session = ARUtils.createArSession(this, installRequested);
          if (session == null) {
            installRequested = ARUtils.hasCameraPermission(this);
            return;
          } else {
            mArSceneView.setupSession(session);
          }
        } catch (UnavailableException e) {
          ARUtils.handleSessionException(this, e);
        }
      }
      try {
        mArSceneView.resume();
      } catch (CameraNotAvailableException e) {
        ARUtils.displayError(this, "The camera cannot be acquired.", e);
      }
    }
  }

  @Override
  protected void onResume(){
    super.onResume();
    mSceneView.resume();
    startArSession();
  }

  /**
   * Determine if we're able to use the camera
   */
  private void checkForCameraPermissions() {
    // Explicitly check for privilege
    if (android.os.Build.VERSION.SDK_INT >= 23) {
      final int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
      if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
        Log.i("MainActivity", "Camera permission granted");
        setUpARScene();

      } else {
        Log.i("MainActivity", "Camera permission not granted, asking ....");
        ActivityCompat.requestPermissions(this,
            new String[] { Manifest.permission.CAMERA },
            PERMISSION_TO_USE_CAMERA);
      }
    }
  }
  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case PERMISSION_TO_USE_CAMERA: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.i("MainActivity", "Camera permission granted...");
          setUpARScene();
        } else {
          Log.i("MainActivity", "Camera permission denied...");
        }
        return;
      }
    }
  }

  @Override
  public void onSceneError(Exception e) {
    String sErr = "";
    if (e instanceof CameraNotAvailableException) sErr = "Could not acquire camera";
    ARUtils.displayError(this, sErr, e);
  }

  @Override
  public void onSceneUpdate(Scene scene, Session session, Frame frame, FrameTime frameTime) {
    Vector3 pos = scene.getCamera().getWorldPosition();
    Quaternion rot = scene.getCamera().getWorldRotation();
    String sPose = getString(R.string.pose, pos.x, pos.y, pos.z, rot.x, rot.y, rot.z, rot.w);
    txtPose.setText(sPose);
  }
}