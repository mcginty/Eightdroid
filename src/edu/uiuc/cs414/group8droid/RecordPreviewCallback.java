package edu.uiuc.cs414.group8droid;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;

import com.google.protobuf.ByteString;

import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket.PacketType;

/**
 * RecordPreviewCallback
 * @author iro
 *
 * Called by Android whenever a frame is captured from the webcam. This will
 * take that frame and do whatever's necessary to get it to the connected agent.
 */
class RecordPreviewCallback implements PreviewCallback {
	private final String TAG = "Eightdroid";
	SkeletonActivity parent;
	
	public RecordPreviewCallback(SkeletonActivity parent) {
		this.parent = parent;
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		/*ByteArrayOutputStream jpegstream = new ByteArrayOutputStream();
		Log.d("Eightdroid", "PreviewFrame retrieved of length: " + data.length);
		
		ByteArrayInputStream bytes = new ByteArrayInputStream(data);
		BitmapDrawable bmd = new BitmapDrawable(bytes);
		Log.d("Eightdroid", "bmd size: " + bmd.getIntrinsicHeight() + ", " + bmd.getIntrinsicWidth());
		Bitmap raw = bmd.getBitmap();
		
		if (raw == null) Log.e("Eightdroid", "Balls. raw is null.");
		raw.compress(Bitmap.CompressFormat.JPEG, 50, jpegstream);
		*/
		int width = camera.getParameters().getPreviewSize().width;
		int height = camera.getParameters().getPreviewSize().height;
		YuvImage yuvimage=new YuvImage(data,ImageFormat.NV21,width,height,null);
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		yuvimage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);

		ByteString buf = ByteString.copyFrom(baos.toByteArray());
		
		DataPacket proto = DataPacket.newBuilder()
			.setTimestamp((new Date()).getTime())
			.setServertime((new Date()).getTime())
			.setType(PacketType.VIDEO)
			.setData(buf).build();
		
		parent.outnet.queuePacket(proto);
		
		Log.d("Eightdroid", "PreviewFrame compressed to a packet of size: " + proto.getSerializedSize());
	}
	
}