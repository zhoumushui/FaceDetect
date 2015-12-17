package com.tchip.facedetect.util;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.tchip.carlauncher.Constant;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class FaceDetectUtil {

	public interface FaceCallBack {
		void success(JSONObject result);

		void error(FaceppParseException exception);
	}

	public static void detect(final Bitmap bitmapDetect,
			final FaceCallBack faceCallBack) {

		new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					// 请求
					HttpRequests httpRequests = new HttpRequests(
							Constant.FacePlusPlus.API_KEY,
							Constant.FacePlusPlus.API_SECRET, true, true);
					// Log.v(TAG, "image size : " + img.getWidth() + " " +
					// img.getHeight());

					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					float scale = Math.min(1, Math.min(
							600f / bitmapDetect.getWidth(),
							600f / bitmapDetect.getHeight()));
					Matrix matrix = new Matrix();
					matrix.postScale(scale, scale);

					Bitmap imgSmall = Bitmap.createBitmap(bitmapDetect, 0, 0,
							bitmapDetect.getWidth(), bitmapDetect.getHeight(),
							matrix, false);
					// Log.v(TAG, "imgSmall size : " + imgSmall.getWidth() + " "
					// + imgSmall.getHeight());

					imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] array = stream.toByteArray();

					PostParameters params = new PostParameters();
					params.setImg(array);

					JSONObject result = httpRequests.detectionDetect(params);
					if (faceCallBack != null) {
						faceCallBack.success(result);
					}
				} catch (FaceppParseException e) {
					e.printStackTrace();
					if (faceCallBack != null) {
						faceCallBack.error(e);
					}
				}
			}
			// try {
			// // detect
			// JSONObject result = httpRequests
			// .detectionDetect(new PostParameters()
			// .setImg(array));
			// // finished , then call the callback function
			// if (callback != null) {
			// callback.detectResult(result);
			// }
			// } catch (FaceppParseException e) {
			// e.printStackTrace();
			// FaceDetectActivity.this.runOnUiThread(new Runnable() {
			// public void run() {
			// textState.setText("Network error.");
			// }
			// });
			// }
			// }
		}).start();

	}

}
