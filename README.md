# pdf-viewer

Pdf-viewer is an Android App that loads and displays a pdf file to the user. To test the App, please feel free to download ./PDFViewer.apk and run on any Android devices. The App is designed for a larger device (i.e., a tablet), but it should also run just as well on a smaller device (i.e., a smartphone)

The viewer allows users to pan and zoom the pdf with two fingers. This is achieved by performing two key matrix transformations, scaling and transforming, onto the pdf itself and all the annotations on each page.

The pdf-viewer also allows users to anotate the pdf with either a pencil or a highlighter. Annotation can be erased with an eraser, or undone/redone with the undo/redo buttoms (achieved by implementing an undo stack and a redo stack). Users could also switch to a different page while keep their annotations on the previous page, so that they could switch back and continue to edit their annotations.

Lastly, the pdf-viewer saves the state of the Android App upon pauses (e.g., switch to another Android App), and resumes the current state when switched back.

# Development Environment
- openjdk 16.0.1 2021-04-20 
- Android API 30 Platform 
- Tested with Android Simulator: Pixel C API 30 
- Microsoft Windows 10 Home 10.0.19042 Build

# Contact
For any feedback, please email to kevin.cj.huang@gmail.com
