package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;
    private OnPageChangeListener listener;
    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        final FloatingActionButton fab = findViewById(R.id.floating_action_button);
        fab.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                startActivity(Intent.createChooser(
                        ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                                                 .setType("text/plain").setText("Some sample text")
                                                 .getIntent(), getString(R.string.action_share)));

            }

        });
        listener = new OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                mCursor.moveToPosition(position);

                String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
                ImageView backdrop = findViewById(R.id.backdrop);
                Glide.with(ArticleDetailActivity.this).load(photoUrl)
                     .transition(DrawableTransitionOptions.withCrossFade()).into(backdrop);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

                switch (state) {
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        fab.hide();
                        break;
                    case ViewPager.SCROLL_STATE_IDLE:
                        fab.show();
                        break;
                }

            }

        };
        mPager.addOnPageChangeListener(listener);

        if (savedInstanceState == null) {

            if (getIntent() != null && getIntent().getData() != null) {

                mStartId = ItemsContract.Items.getItemId(getIntent().getData());

            }

        }

    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return ArticleLoader.newAllArticlesInstance(this);

    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {

        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        mPager.post(new Runnable() {

            @Override
            public void run() {

                listener.onPageSelected(mPager.getCurrentItem());

            }

        });

        // Select the start ID
        if (mStartId > 0) {

            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {

                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {

                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;

                }
                mCursor.moveToNext();

            }
            mStartId = 0;

        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {

        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();

    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        MyPagerAdapter(FragmentManager fm) {

            super(fm);

        }

        @Override
        public Fragment getItem(int position) {

            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));

        }

        @Override
        public int getCount() {

            return (mCursor != null) ? mCursor.getCount() : 0;

        }

    }

}
