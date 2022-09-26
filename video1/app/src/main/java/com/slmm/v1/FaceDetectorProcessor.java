/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.slmm.v1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.slmm.v1.GraphicOverlay;
import com.slmm.v1.VisionProcessorBase;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;
import java.util.Locale;

/** Face Detector Demo. */
public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {

  private static final String TAG = "FaceDetectorProcessor";

  private final FaceDetector detector;

  public FaceDetectorProcessor(Context context) {
    super(context);
    FaceDetectorOptions faceDetectorOptions = getFaceDetectorOptions(context);
    Log.v(MANUAL_TESTING_LOG, "Face detector options: " + faceDetectorOptions);
    detector = FaceDetection.getClient(faceDetectorOptions);
  }

  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  protected Task<List<Face>> detectInImage(InputImage image) {
    return detector.process(image);
  }

  @Override
  protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay) {
    for (Face face : faces) {
      graphicOverlay.add(new FaceGraphic(graphicOverlay, face));
      logExtrasForTesting(face);
    }
  }

  private static void logExtrasForTesting(Face face) {
    if (face != null) {
      Log.v(MANUAL_TESTING_LOG, "face bounding box: " + face.getBoundingBox().flattenToString());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle X: " + face.getHeadEulerAngleX());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Y: " + face.getHeadEulerAngleY());
      Log.v(MANUAL_TESTING_LOG, "face Euler Angle Z: " + face.getHeadEulerAngleZ());

      // All landmarks
      int[] landMarkTypes =
          new int[] {
            FaceLandmark.MOUTH_BOTTOM,
            FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_EAR,
            FaceLandmark.RIGHT_CHEEK,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.NOSE_BASE
          };
      String[] landMarkTypesStrings =
          new String[] {
            "MOUTH_BOTTOM",
            "MOUTH_RIGHT",
            "MOUTH_LEFT",
            "RIGHT_EYE",
            "LEFT_EYE",
            "RIGHT_EAR",
            "LEFT_EAR",
            "RIGHT_CHEEK",
            "LEFT_CHEEK",
            "NOSE_BASE"
          };
      for (int i = 0; i < landMarkTypes.length; i++) {
        FaceLandmark landmark = face.getLandmark(landMarkTypes[i]);
        if (landmark == null) {
          Log.v(
              MANUAL_TESTING_LOG,
              "No landmark of type: " + landMarkTypesStrings[i] + " has been detected");
        } else {
          PointF landmarkPosition = landmark.getPosition();
          String landmarkPositionStr =
              String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y);
          Log.v(
              MANUAL_TESTING_LOG,
              "Position for face landmark: "
                  + landMarkTypesStrings[i]
                  + " is :"
                  + landmarkPositionStr);
        }
      }
      Log.v(
          MANUAL_TESTING_LOG,
          "face left eye open probability: " + face.getLeftEyeOpenProbability());
      Log.v(
          MANUAL_TESTING_LOG,
          "face right eye open probability: " + face.getRightEyeOpenProbability());
      Log.v(MANUAL_TESTING_LOG, "face smiling probability: " + face.getSmilingProbability());
      Log.v(MANUAL_TESTING_LOG, "face tracking id: " + face.getTrackingId());
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Face detection failed " + e);
  }

  private static int getModeTypePreferenceValue(
          Context context, @StringRes int prefKeyResId, int defaultValue) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(prefKeyResId);
    return Integer.parseInt(sharedPreferences.getString(prefKey, String.valueOf(defaultValue)));
  }
  public static FaceDetectorOptions getFaceDetectorOptions(Context context) {
    int landmarkMode =
            getModeTypePreferenceValue(
                    context,
                    R.string.pref_key_live_preview_face_detection_landmark_mode,
                    FaceDetectorOptions.LANDMARK_MODE_NONE);
    int contourMode =
            getModeTypePreferenceValue(
                    context,
                    R.string.pref_key_live_preview_face_detection_contour_mode,
                    FaceDetectorOptions.CONTOUR_MODE_ALL);
    int classificationMode =
            getModeTypePreferenceValue(
                    context,
                    R.string.pref_key_live_preview_face_detection_classification_mode,
                    FaceDetectorOptions.CLASSIFICATION_MODE_NONE);
    int performanceMode =
            getModeTypePreferenceValue(
                    context,
                    R.string.pref_key_live_preview_face_detection_performance_mode,
                    FaceDetectorOptions.PERFORMANCE_MODE_FAST);

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean enableFaceTracking =
            sharedPreferences.getBoolean(
                    context.getString(R.string.pref_key_live_preview_face_detection_face_tracking), false);
    float minFaceSize =
            Float.parseFloat(
                    sharedPreferences.getString(
                            context.getString(R.string.pref_key_live_preview_face_detection_min_face_size),
                            "0.1"));

    FaceDetectorOptions.Builder optionsBuilder =
            new FaceDetectorOptions.Builder()
                    .setLandmarkMode(landmarkMode)
                    .setContourMode(contourMode)
                    .setClassificationMode(classificationMode)
                    .setPerformanceMode(performanceMode)
                    .setMinFaceSize(minFaceSize);
    if (enableFaceTracking) {
      optionsBuilder.enableTracking();
    }
    return optionsBuilder.build();
  }
}
