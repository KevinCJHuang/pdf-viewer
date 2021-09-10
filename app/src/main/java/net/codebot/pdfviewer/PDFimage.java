package net.codebot.pdfviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";

    // drawing path
    Path path = null;
    Action action = null;
    ArrayList<ArrayList<Action>> undos;
    ArrayList<ArrayList<Action>> redos;
    ArrayList<Action> paths = new ArrayList();
    ArrayList<Action> undoPaths = new ArrayList();
    Paint paintbrush = new Paint();
    Bitmap bitmap;

    // constructor
    public PDFimage(Context context) {
        super(context);
        paintbrush.setStyle(Paint.Style.STROKE);
        paintbrush.setColor(Color.BLUE);
        paintbrush.setStrokeWidth(10);
    }

    public void initUndoRedos(int count) {
        undos = new ArrayList<ArrayList<Action>>();
        redos = new ArrayList<ArrayList<Action>>();
        for (int i = 0; i < count; i++) {
            ArrayList<Action> curPage_redos = new ArrayList<Action>();
            ArrayList<Action> curPage_undos = new ArrayList<Action>();
            undos.add(curPage_undos);
            redos.add(curPage_redos);
        }
    }

    public static class Action {
        Path path;
        Paint paintbrush;
        Action(Path path, Paint paintbrush) {
            this.path = path;
            this.paintbrush = paintbrush;
        }
    }

    // we save a lot of points because they need to be processed
    // during touch events e.g. ACTION_MOVE
    float x1, x2, y1, y2, old_x1, old_y1, old_x2, old_y2;
    float mid_x = -1f, mid_y = -1f, old_mid_x = -1f, old_mid_y = -1f;
    int p1_id, p1_index, p2_id, p2_index;

    Matrix matrix = new Matrix();
    Matrix inverse = new Matrix();

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getPointerCount()) {
            // 1 point is drawing or erasing
            case 1:
                p1_id = event.getPointerId(0);
                p1_index = event.findPointerIndex(p1_id);

                // invert using the current matrix to account for pan/scale
                // inverts in-place and returns boolean
                inverse = new Matrix();
                matrix.invert(inverse);

                // mapPoints returns values in-place
                float[] inverted = new float[] { event.getX(p1_index), event.getY(p1_index) };
                inverse.mapPoints(inverted);
                x1 = inverted[0];
                y1 = inverted[1];

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(LOGNAME, "Action down");
                        path = new Path();
                        Paint cur_paintbrush = new Paint(paintbrush);
                        action = new Action(path, cur_paintbrush);
                        paths.add(action);
                        path.moveTo(x1, y1);
                        undoPaths.clear();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(LOGNAME, "Action move");
                        path.lineTo(x1, y1);
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(LOGNAME, "Action up");
                        break;
                }
                break;
            // 2 points is zoom/pan
            case 2:
                // point 1
                p1_id = event.getPointerId(0);
                p1_index = event.findPointerIndex(p1_id);

                // mapPoints returns values in-place
                inverted = new float[] { event.getX(p1_index), event.getY(p1_index) };
                inverse.mapPoints(inverted);

                // first pass, initialize the old == current value
                if (old_x1 < 0 || old_y1 < 0) {
                    old_x1 = x1 = inverted[0];
                    old_y1 = y1 = inverted[1];
                } else {
                    old_x1 = x1;
                    old_y1 = y1;
                    x1 = inverted[0];
                    y1 = inverted[1];
                }

                // point 2
                p2_id = event.getPointerId(1);
                p2_index = event.findPointerIndex(p2_id);

                // mapPoints returns values in-place
                inverted = new float[] { event.getX(p2_index), event.getY(p2_index) };
                inverse.mapPoints(inverted);

                // first pass, initialize the old == current value
                if (old_x2 < 0 || old_y2 < 0) {
                    old_x2 = x2 = inverted[0];
                    old_y2 = y2 = inverted[1];
                } else {
                    old_x2 = x2;
                    old_y2 = y2;
                    x2 = inverted[0];
                    y2 = inverted[1];
                }

                // midpoint
                mid_x = (x1 + x2) / 2;
                mid_y = (y1 + y2) / 2;
                old_mid_x = (old_x1 + old_x2) / 2;
                old_mid_y = (old_y1 + old_y2) / 2;

                // distance
                float d_old = (float) Math.sqrt(Math.pow((old_x1 - old_x2), 2) + Math.pow((old_y1 - old_y2), 2));
                float d = (float) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

                // pan and zoom during MOVE event
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    Log.d(LOGNAME, "Multitouch move");
                    // pan == translate of midpoint
                    float dx = mid_x - old_mid_x;
                    float dy = mid_y - old_mid_y;
                    matrix.preTranslate(dx, dy);
                    Log.d(LOGNAME, "translate: " + dx + "," + dy);

                    // zoom == change of spread between p1 and p2
                    float scale = d/d_old;
                    scale = Math.max(0, scale);
                    matrix.preScale(scale, scale, mid_x, mid_y);
                    Log.d(LOGNAME, "scale: " + scale);

                    // reset on up
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    old_x1 = -1f;
                    old_y1 = -1f;
                    old_x2 = -1f;
                    old_y2 = -1f;
                    old_mid_x = -1f;
                    old_mid_y = -1f;
                }

                break;
            // I have no idea what the user is doing for 3+ points
            default:
                break;
        }
        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // apply transformations from the event handler above
        canvas.concat(matrix);

        // draw background
        this.setImageBitmap(bitmap);
        Paint backgroundColor = new Paint();
        backgroundColor.setColor(Color.WHITE);
        canvas.drawRect(new Rect(0, 0, 1800, 2000), backgroundColor);
        // draw lines over it
        for (Action action : paths) {
            canvas.drawPath(action.path, action.paintbrush);
        }

        super.onDraw(canvas);
    }

    public void undo() {
        if (paths.size()>0) {
            undoPaths.add(paths.remove(paths.size() - 1));
        }
    }

    public void redo() {
        if (undoPaths.size() > 0) {
            paths.add(undoPaths.remove(undoPaths.size() - 1));
        }
    }
}
