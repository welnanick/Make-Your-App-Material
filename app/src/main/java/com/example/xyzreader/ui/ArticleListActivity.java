package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ArticleLoader.Query;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = findViewById(R.id.recycler_view);
        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {

            refresh();

        }

    }

    private void refresh() {

        startService(new Intent(this, UpdaterService.class));

    }

    @Override
    protected void onStart() {

        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));

    }

    @Override
    protected void onStop() {

        super.onStop();
        unregisterReceiver(mRefreshingReceiver);

    }

    private boolean mIsRefreshing = false;
    private final BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {

                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();

            }

        }

    };

    private void updateRefreshingUI() {

        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);

    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return ArticleLoader.newAllArticlesInstance(this);

    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {

        Adapter adapter = new Adapter(cursor, this);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

        mRecyclerView.setAdapter(null);

    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private final Cursor mCursor;
        private final Context context;

        Adapter(Cursor cursor, Context context) {

            mCursor = cursor;
            this.context = context;

        }

        @Override
        public long getItemId(int position) {

            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);

        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));

                }

            });
            return vh;

        }

        private Date parsePublishedDate() {

            try {

                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);

            }
            catch (ParseException ex) {

                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();

            }

        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(getString(R.string.byline_placeholder_linebreak,
                        DateUtils.getRelativeTimeSpanString(publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString(),
                        mCursor.getString(ArticleLoader.Query.AUTHOR)));

            }
            else {

                holder.subtitleView.setText(getString(R.string.byline_placeholder_linebreak,
                        outputFormat.format(publishedDate),
                        mCursor.getString(ArticleLoader.Query.AUTHOR)));

            }
            Glide.with(context).load(mCursor.getString(Query.PHOTO_URL))
                 .transition(DrawableTransitionOptions.withCrossFade()).into(holder.thumbnailView);

        }

        @Override
        public int getItemCount() {

            return mCursor.getCount();

        }

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView thumbnailView;
        final TextView titleView;
        final TextView subtitleView;

        ViewHolder(View view) {

            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);

        }

    }

}
