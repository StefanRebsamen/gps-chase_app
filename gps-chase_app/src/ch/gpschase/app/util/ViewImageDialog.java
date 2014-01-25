package ch.gpschase.app.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import ch.gpschase.app.App;
import ch.gpschase.app.R;
import ch.gpschase.app.data.Image;

/**
 * A dialog to display an image
 */
public class ViewImageDialog {

	public static void show(Context context, Image image) { 
		// create an image view
		ImageView imageView = new ImageView(context);
		imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		imageView.setPadding(8, 8, 8, 8);
		imageView.setAdjustViewBounds(true);
		imageView.setImageBitmap(App.getImageManager().getFull(image));
	
		// show it in an dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_show_image_title);
		builder.setIcon(R.drawable.ic_image);
		builder.setView(imageView);
		builder.setPositiveButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}
}
