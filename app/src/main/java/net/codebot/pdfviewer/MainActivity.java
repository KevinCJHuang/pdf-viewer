package net.codebot.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so we should expect people to need this.
// We may wish to provide this code.

public class MainActivity extends AppCompatActivity {
    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "shannon1948.pdf";
    final int FILERESID = R.raw.shannon1948;
    int curIndex = 0;
    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout layout = findViewById(R.id.pdfLayout);
        pageImage = new PDFimage(this);
        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            showPage(0); // show first page
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }
        pageImage.initUndoRedos(pdfRenderer.getPageCount());
        initializeEvents();
        TextView fileName = findViewById(R.id.pdfName);
        fileName.setText(FILENAME);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initializeEvents() {
        ImageButton brushBtn = findViewById(R.id.brush);
        ImageButton highlighterBtn = findViewById(R.id.highlighter);
        ImageButton eraserBtn = findViewById(R.id.eraser);
        brushBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageImage.paintbrush.setColor(Color.BLUE);
                pageImage.paintbrush.setStrokeWidth(10);
            }
        });
        highlighterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageImage.paintbrush.setColor(Color.YELLOW);
                pageImage.paintbrush.setStrokeWidth(50);
                pageImage.paintbrush.setAlpha(50);
            }
        });
        eraserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageImage.paintbrush.setColor(Color.WHITE);
                pageImage.paintbrush.setStrokeWidth(50);
            }
        });

        ImageButton leftArrowBtn = findViewById(R.id.left_arrow);
        ImageButton rightArrowBtn = findViewById(R.id.right_arrow);
        final TextView pageNum = findViewById(R.id.pageNum);
        pageNum.setText((curIndex+1) + "/" + pdfRenderer.getPageCount());
        leftArrowBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                try {
                    if (curIndex >= 1){
                        showPage(--curIndex); // show first page
                        pageImage.paths = pageImage.undos.get(curIndex);
                        pageImage.undoPaths = pageImage.redos.get(curIndex);
                        pageNum.setText((curIndex + 1) + "/" + pdfRenderer.getPageCount());
                    }

                } catch (Exception exception) {
                    Log.d(LOGNAME, "Error opening PDF");
                }
            }
        });
        rightArrowBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                try {
                    if (pdfRenderer.getPageCount() > curIndex + 1) {
                        showPage(++curIndex); // show first page
                        pageImage.paths = pageImage.undos.get(curIndex);
                        pageImage.undoPaths = pageImage.redos.get(curIndex);
                        pageNum.setText((curIndex + 1) + "/" + pdfRenderer.getPageCount());
                    }
                } catch (Exception exception) {
                    Log.d(LOGNAME, "Error opening PDF", exception);
                }
            }
        });

        ImageButton undoBtn = findViewById(R.id.undo);
        ImageButton redoBtn = findViewById(R.id.redo);
        undoBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                pageImage.undo();
            }
        });
        redoBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                pageImage.redo();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        System.out.println(index);
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        System.out.println("setting bitmap page: " +  curIndex);
        // Display the page
        pageImage.setImage(bitmap);
    }
}
