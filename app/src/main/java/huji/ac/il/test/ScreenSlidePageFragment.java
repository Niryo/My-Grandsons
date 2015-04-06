package huji.ac.il.test;


import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import uk.co.senab.photoview.PhotoViewAttacher;


public class ScreenSlidePageFragment extends Fragment {

    private int currentPage;
    private String fileName;
    private ViewGroup rootView = null;
    private Button button;
    private int msgCounter = 1;


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
    public void onPause() {
        super.onPause();
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
                parent.post(new Runnable() {
                    public void run() {
                        final Rect rect = new Rect();
                        playButton.getHitRect(rect);
                        rect.top -= 100;    // increase top hit area
                        rect.left -= 100;   // increase left hit area
                        rect.bottom += 100; // increase bottom hit area
                        rect.right += 100;  // increase right hit area
                        parent.setTouchDelegate(new TouchDelegate(rect, playButton));
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
            if (name.endsWith(".txt")) {
                rootView = (ViewGroup) inflater
                        .inflate(R.layout.fragment_text_layout, container, false);
                Log.w("customMsg", "reading text file");

                File file = new File(name);
                StringBuilder text = new StringBuilder();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    br.close();
                } catch (Exception e) {
                    Log.w("customMsg", "can't read text file");
                }


                TextView text_view = (TextView) rootView.findViewById(R.id.textView);
                text_view.setText(text);
            }

        }

        return rootView;

    }


    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void attachButton(final int lastPage, final ViewPager mPager) {
        LayoutInflater inflater =
                (LayoutInflater) getActivity().getApplicationContext().getSystemService(getActivity().getApplicationContext().LAYOUT_INFLATER_SERVICE);
        this.button = (Button) rootView.findViewById(R.id.new_message_button);
        if (this.button == null) {

            inflater.inflate(R.layout.new_messages_button, rootView, true);
            this.button = (Button) rootView.findViewById(R.id.new_message_button);
            this.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPager.setCurrentItem(lastPage);

                }
            });

//           this.button.startAnimation(AnimationUtils.loadAnimation(getActivity(),R.anim.slide_in_top));
            this.button.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_left));
            this.button.setVisibility(View.VISIBLE);
        }
        this.button.setText(this.msgCounter + " New!");
        this.msgCounter++;
    }

    public void removeButton() {
        if (this.button != null) {
            ViewGroup parent = (ViewGroup) this.button.getParent();
            parent.removeView(this.button);
            this.msgCounter = 1;
        }

    }


}