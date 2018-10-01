package org.sairaa.news360degree;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.sairaa.news360degree.db.News;
import org.sairaa.news360degree.utils.CheckConnection;
import org.sairaa.news360degree.utils.CommonUtils;
import org.sairaa.news360degree.utils.DialogAction;

public class SearchActivity extends AppCompatActivity {
    private EditText searchText;
    private RecyclerView recyclerView;
    private NewsAdapter adapter;
    private NewsViewModel viewModel;
    static DialogAction dialogAction;
    CommonUtils commonUtils;
    FloatingActionButton floatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        floatingActionButton = findViewById(R.id.search_fab);
        dialogAction = new DialogAction(this);
        commonUtils = new CommonUtils(this);
        recyclerView = findViewById(R.id.search_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        viewModel = ViewModelProviders.of(this).get(NewsViewModel.class);
        adapter = new NewsAdapter(this);

        searchText = findViewById(R.id.search_input);
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i == EditorInfo.IME_ACTION_SEARCH){
                    subscribeUi(adapter,searchText.getText().toString().trim());
                    //Close the keyboard
                    View view = SearchActivity.this.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
                return false;
            }
        });

        Toolbar toolbar = findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if(getSupportActionBar() != null){
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle("Search");
        }

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recyclerView.smoothScrollToPosition(0);

            }
        });

    }

    private void subscribeUi(final NewsAdapter adapter, String queryString) {
        viewModel.getSearchNewsListLiveData(queryString).observe(this, new Observer<PagedList<News>>() {
            @Override
            public void onChanged(@Nullable PagedList<News> news) {
                adapter.submitList(news);
                recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(0);

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
