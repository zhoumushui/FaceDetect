package com.tchip.facedetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.ImageColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;
import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.service.SpeakService;
import com.tchip.carlauncher.util.FaceDetectUtil;
import com.tchip.carlauncher.util.NetworkUtil;

public class FaceDetectActivity extends Activity {

	final private static String TAG = "FaceDetect";
	final private int PICTURE_CHOOSE = 1;

	private ImageView imagePhoto = null;
	private Bitmap bitmapPhoto = null;
	private Button btnDetect, btnShare;
	private TextView textState, textAge;
	private View frameWait;
	private TextView textHint;
	private ImageView imageHintArrow1, imageHintArrow2, imageHintArrow3;
	private ProgressBar detectProgress;
	private LinearLayout layoutGuide, layoutBack;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);

		Button btnGetImage = (Button) findViewById(R.id.btnGetImage);
		btnGetImage.setOnClickListener(new MyOnClickListener());

		btnShare = (Button) findViewById(R.id.btnShare);
		btnShare.setOnClickListener(new MyOnClickListener());

		frameWait = findViewById(R.id.frameWait);

		textState = (TextView) findViewById(R.id.textState);

		btnDetect = (Button) findViewById(R.id.btnDetect);
		btnDetect.setVisibility(View.INVISIBLE);
		btnDetect.setOnClickListener(new MyOnClickListener());

		imagePhoto = (ImageView) findViewById(R.id.imagePhoto);
		imagePhoto.setImageBitmap(bitmapPhoto);

		textHint = (TextView) findViewById(R.id.textHint);
		imageHintArrow1 = (ImageView) findViewById(R.id.imageHintArrow1);
		imageHintArrow2 = (ImageView) findViewById(R.id.imageHintArrow2);
		imageHintArrow3 = (ImageView) findViewById(R.id.imageHintArrow3);
		imageHintArrow1.setVisibility(View.VISIBLE);
		imageHintArrow2.setVisibility(View.INVISIBLE);
		imageHintArrow3.setVisibility(View.INVISIBLE);

		textAge = (TextView) frameWait.findViewById(R.id.textAge);
		detectProgress = (ProgressBar) findViewById(R.id.detectProgress);

		layoutBack = (LinearLayout) findViewById(R.id.layoutBack);
		layoutBack.setOnClickListener(new MyOnClickListener());
		layoutGuide = (LinearLayout) findViewById(R.id.layoutGuide);
	}

	class MyOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btnGetImage:
				// 读取图库图片
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, PICTURE_CHOOSE);
				layoutGuide.setVisibility(View.GONE);
				break;
			case R.id.btnDetect:
				if (-1 == NetworkUtil.getNetworkType(getApplicationContext())) {
					NetworkUtil.noNetworkHint(getApplicationContext());
				} else {
					// frameWait.setVisibility(View.VISIBLE);
					detectProgress.setVisibility(View.VISIBLE);
					// textAge.setVisibility(View.INVISIBLE);
					FaceDetectUtil.detect(bitmapPhoto,
							new FaceDetectUtil.FaceCallBack() {

								@Override
								public void success(JSONObject result) {
									Message msg = Message.obtain();
									msg.what = MSG_SUCCESS;
									msg.obj = result;
									faceHandler.sendMessage(msg);
								}

								@Override
								public void error(FaceppParseException exception) {
									Message msg = Message.obtain();
									msg.what = MSG_ERROR;
									msg.obj = exception.getErrorMessage();
									faceHandler.sendMessage(msg);
								}
							});
				}
				break;
			case R.id.layoutBack:
				backToMultimedia();
				break;
			case R.id.btnShare:
				RelativeLayout layoutNotShareArea = (RelativeLayout) findViewById(R.id.layoutNotShareArea);
				layoutNotShareArea.setVisibility(View.INVISIBLE);

				RelativeLayout layoutFaceDetect = (RelativeLayout) findViewById(R.id.layoutFaceDetect);
				Bitmap shareBitmap = createViewBitmap(layoutFaceDetect);
				File file = new File(Constant.RouteTrack.PATH
						+ "face_detect_share" + ".png");
				FileOutputStream out;
				try {
					out = new FileOutputStream(file);
					if (shareBitmap.compress(Bitmap.CompressFormat.PNG, 100,
							out)) {
						out.flush();
						out.close();
					}
					Intent intent = new Intent(Intent.ACTION_SEND);
					Uri uri = Uri.fromFile(file);
					intent.setType("image/png");
					intent.putExtra(Intent.EXTRA_STREAM, uri);
					intent.putExtra(Intent.EXTRA_TITLE, "分享图片到");
					Intent chooserIntent = Intent.createChooser(intent, "分享图片");
					if (chooserIntent == null) {
						return;
					}
					try {
						startActivity(chooserIntent);
					} catch (android.content.ActivityNotFoundException ex) {
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				layoutNotShareArea.setVisibility(View.VISIBLE);
				break;

			default:
				break;
			}
		}
	}

	public Bitmap createViewBitmap(View v) {
		Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		v.draw(canvas);
		return bitmap;
	}

	private static final int MSG_SUCCESS = 0x111;
	private static final int MSG_ERROR = 0x112;
	private Handler faceHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SUCCESS:
				// frameWait.setVisibility(View.GONE);

				JSONObject rst = (JSONObject) msg.obj;
				Log.e(TAG, rst.toString());
				prepareBitmap(rst);
				imagePhoto.setImageBitmap(bitmapPhoto);
				break;
			case MSG_ERROR:
				// frameWait.setVisibility(View.GONE);
				String errorMsg = (String) msg.obj;
				if (TextUtils.isEmpty(errorMsg)) {
					textState.setText("Error");
				} else {
					textState.setText(errorMsg);
				}
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	private Bitmap getGendorBitmap(int age, boolean isMale) {
		textAge.setVisibility(View.VISIBLE);
		textAge.setText("" + age);
		if (isMale) {
			textAge.setTextColor(0xff00479d);
			textAge.setCompoundDrawablesWithIntrinsicBounds(getResources()
					.getDrawable(R.drawable.ui_face_sex_male), null, null, null);
		} else {
			textAge.setTextColor(0xffeb6877);
			textAge.setCompoundDrawablesWithIntrinsicBounds(getResources()
					.getDrawable(R.drawable.ui_face_sex_female), null, null,
					null);
		}
		textAge.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(textAge.getDrawingCache());
		textAge.setVisibility(View.INVISIBLE);
		textAge.destroyDrawingCache();
		return bitmap;
	}

	public void backToMultimedia() {
		finish();

	}

	protected void prepareBitmap(JSONObject rst) {
		// 画笔
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		// paint.setStrokeWidth(Math.max(img.getWidth(),
		// img.getHeight()) / 100f);
		paint.setStrokeWidth(3);

		// 画布
		Bitmap bitmap = Bitmap.createBitmap(bitmapPhoto.getWidth(),
				bitmapPhoto.getHeight(), bitmapPhoto.getConfig());
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(bitmapPhoto, new Matrix(), null);
		try {
			// find out all faces
			JSONArray faceArray = rst.getJSONArray("face");
			final int faceCount = faceArray.length();
			btnShare.setVisibility(View.VISIBLE);
			detectProgress.setVisibility(View.GONE);
			if (faceCount > 0) {
				textHint.setText("识别出" + faceCount + "个脸部，分享照片");
				imageHintArrow1.setVisibility(View.INVISIBLE);
				imageHintArrow2.setVisibility(View.INVISIBLE);
				imageHintArrow3.setVisibility(View.VISIBLE);
				textAge.setVisibility(View.INVISIBLE);
				for (int i = 0; i < faceCount; ++i) {
					float x, y, w, h;
					// 获取人脸中心
					JSONObject faceObject = faceArray.getJSONObject(i);
					JSONObject positionObject = faceObject
							.getJSONObject("position");
					x = (float) positionObject.getJSONObject("center")
							.getDouble("x");
					y = (float) faceObject.getJSONObject("position")
							.getJSONObject("center").getDouble("y");

					// 获取人脸大小
					w = (float) positionObject.getDouble("width");
					h = (float) positionObject.getDouble("height");

					// 绘制人脸方框
					x = x / 100 * bitmapPhoto.getWidth();
					y = y / 100 * bitmapPhoto.getHeight();
					w = w / 100 * bitmapPhoto.getWidth();
					h = h / 100 * bitmapPhoto.getHeight();
					canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2,
							paint);
					canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2,
							paint);
					canvas.drawLine(x + w / 2, y - h / 2, x + w / 2, y + h / 2,
							paint);
					canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2,
							paint);

					// 性别和年龄
					int age = faceObject.getJSONObject("attribute")
							.getJSONObject("age").getInt("value");
					int range = faceObject.getJSONObject("attribute")
							.getJSONObject("age").getInt("range");
					String gendorStr = faceObject.getJSONObject("attribute")
							.getJSONObject("gender").getString("value"); // Male-Female
					Bitmap ageBitmap = getGendorBitmap(age + range,
							"Male".equals(gendorStr));

					int ageWidth = ageBitmap.getWidth();
					int ageHeight = ageBitmap.getHeight();

					if (bitmap.getWidth() < imagePhoto.getWidth()
							&& bitmap.getHeight() < imagePhoto.getHeight()) {
						float ratio = Math.max(bitmap.getWidth() * 1.0f
								/ imagePhoto.getWidth(), bitmap.getHeight()
								* 1.0f / imagePhoto.getHeight());
						ageBitmap = Bitmap.createScaledBitmap(ageBitmap,
								(int) (ageWidth * ratio),
								(int) (ageHeight * ratio), false);
					}

					canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth() / 2,
							y - h / 2 - ageBitmap.getHeight(), null);
				}
			} else {
				String strNoFace = "未识别到脸部，换张照片试试";
				textHint.setText(strNoFace);
				startSpeak(strNoFace);
				imageHintArrow1.setVisibility(View.VISIBLE);
				imageHintArrow2.setVisibility(View.INVISIBLE);
				imageHintArrow3.setVisibility(View.INVISIBLE);
				btnDetect.setVisibility(View.INVISIBLE);
				btnShare.setVisibility(View.INVISIBLE);
			}

			// save new image
			bitmapPhoto = bitmap;

		} catch (JSONException e) {
			e.printStackTrace();
			FaceDetectActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					textState.setText("Error.");
				}
			});
		}
	}

	private void startSpeak(String content) {
		Intent intent = new Intent(FaceDetectActivity.this, SpeakService.class);
		intent.putExtra("content", content);
		startService(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		// the image picker callback
		if (requestCode == PICTURE_CHOOSE) {
			if (intent != null) {
				// Log.d(TAG, "idButSelPic Photopicker: " +
				// intent.getDataString());
				Cursor cursor = getContentResolver().query(intent.getData(),
						null, null, null, null);
				cursor.moveToFirst();
				int idx = cursor.getColumnIndex(ImageColumns.DATA);
				String fileSrc = cursor.getString(idx);
				// Log.d(TAG, "Picture:" + fileSrc);

				// just read size
				Options options = new Options();
				options.inJustDecodeBounds = true;
				bitmapPhoto = BitmapFactory.decodeFile(fileSrc, options);

				// scale size to read
				options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
						(double) options.outWidth / 1024f,
						(double) options.outHeight / 1024f)));
				options.inJustDecodeBounds = false;
				bitmapPhoto = BitmapFactory.decodeFile(fileSrc, options);
				textState.setText("Clik Detect. ==>");

				imagePhoto.setImageBitmap(bitmapPhoto);
				btnDetect.setVisibility(View.VISIBLE);
				textHint.setText("开始人脸年龄识别");
				imageHintArrow1.setVisibility(View.INVISIBLE);
				imageHintArrow2.setVisibility(View.VISIBLE);
				imageHintArrow3.setVisibility(View.INVISIBLE);
			} else {
				Log.d(TAG, "idButSelPic Photopicker canceled");
			}
		}
	}

	// private class FaceppDetect {
	// DetectCallback callback = null;
	//
	// public void setDetectCallback(DetectCallback detectCallback) {
	// callback = detectCallback;
	// }
	//
	// public void detect(final Bitmap image) {
	//
	// new Thread(new Runnable() {
	//
	// public void run() {
	// // zj: old position
	//
	// }
	// }).start();
	// }
	// }

	interface DetectCallback {
		void detectResult(JSONObject rst);
	}
}
