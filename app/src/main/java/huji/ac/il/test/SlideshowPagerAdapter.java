package huji.ac.il.test;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * The adapter for the viewPager
 */
public class SlideshowPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<String> fileNames;
    //holds all the fragment:
    SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();



    public SlideshowPagerAdapter(FragmentManager fragmentManager, ArrayList<String> fileNameList) {
        super(fragmentManager);
        this.fileNames=fileNameList;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        registeredFragments.removeAt(position);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        //save the fragment:
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

    /**
     * Returns the fragment at the given position
     * @param position position
     * @return fragment
     */
    public Fragment getFragment(int position) {
        return registeredFragments.get(position);
    }
}
