package eu.anifantakis.bakingapp.activities;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;

import java.util.List;

import eu.anifantakis.bakingapp.R;
import eu.anifantakis.bakingapp.data.MainDataAdapter;
import eu.anifantakis.bakingapp.data.RequestInterface;
import eu.anifantakis.bakingapp.data.model.Recipe;
import eu.anifantakis.bakingapp.databinding.ActivityMainBinding;
import eu.anifantakis.bakingapp.utils.AppUtils;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private Context context;
    private ActivityMainBinding binding;
    private MainDataAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        setTitle(getString(R.string.title_activity_main));

        AppUtils.setIdleResourceTo(false);

        context = this;
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        binding.rvMaster.setLayoutManager(layoutManager);

        // allow swipe to refresh
        binding.mainLayoutSwipe.setOnRefreshListener(this);

        loadJSON();
    }

    private void loadJSON() {
        binding.mainLayoutSwipe.setRefreshing(true);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppUtils.BASE_URL)
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RequestInterface request = retrofit.create(RequestInterface.class);
        Call<List<Recipe>> call = request.getJSON();

        Log.d("JSON CALL", "RETROFIT CALL");
        call.enqueue(new Callback<List<Recipe>>() {
            @Override
            public void onResponse(@NonNull Call<List<Recipe>> call, @NonNull Response<List<Recipe>> response) {

                Log.i("JSON RESPONSE", "SUCCESS");
                List<Recipe> jsonResponse = response.body();

                adapter = new MainDataAdapter(jsonResponse, clickedItemIndex -> {
                    Recipe recipe = adapter.getRecipeAtIndex(clickedItemIndex);
                    Intent intent = new Intent(context, RecipeInfoActivity.class);
                    intent.putExtra(AppUtils.EXTRAS_RECIPE, recipe);
                    startActivityForResult(intent, 1);
                });
                binding.rvMaster.setAdapter(adapter);
                binding.mainLayoutSwipe.setRefreshing(false);

                AppUtils.setIdleResourceTo(true);
            }

            @Override
            public void onFailure(@NonNull Call<List<Recipe>> call, @NonNull Throwable t) {
                Log.e("JSON RESPONSE", "FAIL: " + t.getMessage());
                binding.mainLayoutSwipe.setRefreshing(false);

                // if we have a network error, prompt a dialog asking to retry or exit
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(R.string.main_no_network)
                        .setNegativeButton(R.string.main_no_network_try_again, (dialog, id) -> loadJSON())
                        .setPositiveButton(R.string.main_no_network_close, (dialog, id) -> finish());
                builder.create().show();

                AppUtils.setIdleResourceTo(false);
            }
        });
    }

    @Override
    public void onRefresh() {
        loadJSON();
    }
}
