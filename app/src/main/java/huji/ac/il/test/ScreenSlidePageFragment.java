package huji.ac.il.test;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import uk.co.senab.photoview.PhotoViewAttacher;


public class ScreenSlidePageFragment extends Fragment {

    private int currentPage;
    private String fileName;
    private ViewGroup rootView=null;



    /**
     * Factory method for this fragment class. Constructs a new fragment.
     */
    public static ScreenSlidePageFragment create(String fileName) {
        ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
        fragment.setFileName(fileName);


        return fragment;
    }

    public ScreenSlidePageFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

@Override
public void onPause(){
    super.onPause();
    if(this.rootView!=null){

    }
}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.


        // Set the title view to show the page number.
        String name = this.fileName;
//        ViewGroup rootView=null;
        if (name != null) {
            if (name.endsWith(".jpg")) {
                 rootView = (ViewGroup) inflater
                        .inflate(R.layout.fragment_image_layout, container, false);

                Bitmap myBitmap = BitmapFactory.decodeFile(name);
                ImageView view = (ImageView) rootView.findViewById(R.id.imageView);
                view.setImageBitmap(myBitmap);
                PhotoViewAttacher mAttacher = new PhotoViewAttacher(view);
            }
            if (name.endsWith(".mp4")) {
                rootView = (ViewGroup) inflater
                        .inflate(R.layout.fragment_video_layout, container, false);

               final VideoView video = (VideoView) rootView.findViewById(R.id.videoView);
//                final MediaController mc = new MediaController(getActivity());
//                mc.setAnchorView(video);
//                video.setMediaController(mc);
                video.setVideoURI(Uri.parse(name));
                video.requestFocus();
                final Button playButton = (Button) rootView.findViewById(R.id.playButton);
                playButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        v.setVisibility(View.INVISIBLE);
                        video.start();

                    }
                });

                final View parent = (View) playButton.getParent();  // button: the view you want to enlarge hit area
                parent.post( new Runnable() {
                    public void run() {
                        final Rect rect = new Rect();
                        playButton.getHitRect(rect);
                        rect.top -= 100;    // increase top hit area
                        rect.left -= 100;   // increase left hit area
                        rect.bottom += 100; // increase bottom hit area
                        rect.right += 100;  // increase right hit area
                        parent.setTouchDelegate( new TouchDelegate( rect , playButton));
                    }
                });

                video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                      playButton.setVisibility(View.VISIBLE);
                    }
                });

                video.seekTo(100);


            }

        }
        return rootView;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public void setCurrentPage(int position){
        this.currentPage=position;
    }
    public int getPagenage(){
        return this.currentPage;
    }


}