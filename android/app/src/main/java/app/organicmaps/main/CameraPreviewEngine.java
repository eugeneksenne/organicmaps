package app.organicmaps.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Lifecycle-aware Camera2 preview and still-capture engine. Images are written to the app cache so
 * the upload queue can take ownership of them without exposing a partially captured file.
 */
final class CameraPreviewEngine
{
  interface Listener
  {
    void onPhotoCaptured(@NonNull File file);
    void onCameraError(@NonNull String message);
  }

  private final Activity mActivity;
  private final TextureView mPreview;
  private final Listener mListener;
  private final CameraManager mCameraManager;
  private HandlerThread mCameraThread;
  private Handler mCameraHandler;
  private CameraDevice mCamera;
  private CameraCaptureSession mSession;
  private ImageReader mImageReader;
  private String mCameraId;
  private Size mPhotoSize;

  CameraPreviewEngine(@NonNull Activity activity, @NonNull TextureView preview, @NonNull Listener listener)
  {
    mActivity = activity;
    mPreview = preview;
    mListener = listener;
    mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    mPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener()
    {
      @Override public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) { open(); }
      @Override public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}
      @Override public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) { close(); return true; }
      @Override public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
    });
  }

  void start()
  {
    if (mCameraThread != null)
      return;
    mCameraThread = new HandlerThread("FomoCamera");
    mCameraThread.start();
    mCameraHandler = new Handler(mCameraThread.getLooper());
    if (mPreview.isAvailable())
      open();
  }

  void stop()
  {
    close();
    if (mCameraThread != null)
    {
      mCameraThread.quitSafely();
      mCameraThread = null;
      mCameraHandler = null;
    }
  }

  @SuppressLint("MissingPermission")
  void open()
  {
    if (mCameraHandler == null || mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
      return;
    try
    {
      selectBackCamera();
      mCameraManager.openCamera(mCameraId, mDeviceCallback, mCameraHandler);
    }
    catch (CameraAccessException | IllegalArgumentException e)
    {
      mListener.onCameraError("Camera is unavailable");
    }
  }

  void capture()
  {
    if (mCamera == null || mSession == null || mImageReader == null)
    {
      mListener.onCameraError("Camera is still starting");
      return;
    }
    try
    {
      final CaptureRequest.Builder request = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      request.addTarget(mImageReader.getSurface());
      request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
      request.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation());
      mSession.stopRepeating();
      mSession.capture(request.build(), new CameraCaptureSession.CaptureCallback()
      {
        @Override public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber)
        {
          startPreview();
        }
      }, mCameraHandler);
    }
    catch (CameraAccessException e)
    {
      mListener.onCameraError("Could not capture photo");
    }
  }

  private void selectBackCamera() throws CameraAccessException
  {
    for (String id : mCameraManager.getCameraIdList())
    {
      final CameraCharacteristics details = mCameraManager.getCameraCharacteristics(id);
      final Integer facing = details.get(CameraCharacteristics.LENS_FACING);
      if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK)
      {
        configureCamera(id, details);
        return;
      }
    }
    throw new CameraAccessException(CameraAccessException.CAMERA_ERROR);
  }

  private void configureCamera(@NonNull String id, @NonNull CameraCharacteristics details)
  {
    mCameraId = id;
    final StreamConfigurationMap map = details.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    if (map == null)
      throw new IllegalArgumentException("Camera has no stream configuration");
    mPhotoSize = largest(map.getOutputSizes(ImageFormat.JPEG));
    mImageReader = ImageReader.newInstance(mPhotoSize.getWidth(), mPhotoSize.getHeight(), ImageFormat.JPEG, 2);
    mImageReader.setOnImageAvailableListener(this::saveImage, mCameraHandler);
  }

  @NonNull
  private static Size largest(@NonNull Size[] sizes)
  {
    Size result = sizes[0];
    for (Size size : sizes)
      if ((long) size.getWidth() * size.getHeight() > (long) result.getWidth() * result.getHeight())
        result = size;
    return result;
  }

  private final CameraDevice.StateCallback mDeviceCallback = new CameraDevice.StateCallback()
  {
    @Override public void onOpened(@NonNull CameraDevice camera) { mCamera = camera; startPreview(); }
    @Override public void onDisconnected(@NonNull CameraDevice camera) { camera.close(); mCamera = null; }
    @Override public void onError(@NonNull CameraDevice camera, int error) { camera.close(); mCamera = null; mListener.onCameraError("Camera error"); }
  };

  private void startPreview()
  {
    if (mCamera == null || !mPreview.isAvailable() || mImageReader == null)
      return;
    try
    {
      final SurfaceTexture texture = mPreview.getSurfaceTexture();
      texture.setDefaultBufferSize(mPhotoSize.getWidth(), mPhotoSize.getHeight());
      final Surface surface = new Surface(texture);
      final CaptureRequest.Builder request = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      request.addTarget(surface);
      request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
      mCamera.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback()
      {
        @Override public void onConfigured(@NonNull CameraCaptureSession session)
        {
          mSession = session;
          try { session.setRepeatingRequest(request.build(), null, mCameraHandler); }
          catch (CameraAccessException e) { mListener.onCameraError("Could not start preview"); }
        }
        @Override public void onConfigureFailed(@NonNull CameraCaptureSession session) { mListener.onCameraError("Could not configure camera"); }
      }, mCameraHandler);
    }
    catch (CameraAccessException e)
    {
      mListener.onCameraError("Could not start preview");
    }
  }

  private void saveImage(@NonNull ImageReader reader)
  {
    Image image = null;
    try
    {
      image = reader.acquireNextImage();
      final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
      final byte[] bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      final File directory = new File(mActivity.getCacheDir(), "moments");
      if (!directory.exists() && !directory.mkdirs())
        throw new IOException("Unable to create moment directory");
      final String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
      final File output = new File(directory, "moment_" + timestamp + ".jpg");
      try (FileOutputStream stream = new FileOutputStream(output)) { stream.write(bytes); }
      mActivity.runOnUiThread(() -> mListener.onPhotoCaptured(output));
    }
    catch (IOException | RuntimeException e)
    {
      mActivity.runOnUiThread(() -> mListener.onCameraError("Could not save photo"));
    }
    finally
    {
      if (image != null)
        image.close();
    }
  }

  private int getJpegOrientation()
  {
    final int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
    final int degrees = rotation == Surface.ROTATION_0 ? 0 : rotation == Surface.ROTATION_90 ? 90 : rotation == Surface.ROTATION_180 ? 180 : 270;
    return (mCameraManager == null ? 0 : (degrees + 90) % 360);
  }

  private void close()
  {
    if (mSession != null) { mSession.close(); mSession = null; }
    if (mCamera != null) { mCamera.close(); mCamera = null; }
    if (mImageReader != null) { mImageReader.close(); mImageReader = null; }
  }
}
