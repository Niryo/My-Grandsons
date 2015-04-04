package huji.ac.il.test;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.BoringLayout;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.ArrayList;


public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<String> fileNames;
    SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();



    public ScreenSlidePagerAdapter(FragmentManager fragmentManager, ArrayList<String> fileNameList) {
        super(fragmentManager);
        this.fileNames=fileNameList;

    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment newFragment= ScreenSlidePageFragment.create(fileNames.get(position));
        return newFragment;
    }

    @Override
    public int getCount() {
        return fileNames.size();
    }

    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }
}
