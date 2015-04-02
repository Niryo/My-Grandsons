package huji.ac.il.test;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.BoringLayout;

import java.util.ArrayList;


public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<String> fileNames;



    public ScreenSlidePagerAdapter(FragmentManager fragmentManager, ArrayList<String> fileNameList) {
        super(fragmentManager);
        this.fileNames=fileNameList;

    }


    @Override
    public Fragment getItem(int position) {
        return ScreenSlidePageFragment.create(fileNames.get(position));
    }

    @Override
    public int getCount() {
        return fileNames.size();
    }
}
