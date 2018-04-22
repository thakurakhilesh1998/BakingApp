package eu.anifantakis.bakingapp.activities;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;

import eu.anifantakis.bakingapp.BakingWidgetProvider;
import eu.anifantakis.bakingapp.BuildConfig;
import eu.anifantakis.bakingapp.R;
import eu.anifantakis.bakingapp.data.model.Ingredient;
import eu.anifantakis.bakingapp.data.model.Recipe;
import eu.anifantakis.bakingapp.data.model.Step;
import eu.anifantakis.bakingapp.fragments.RecipeInfoDetailFragment;
import eu.anifantakis.bakingapp.fragments.RecipeInfoFragment;
import eu.anifantakis.bakingapp.utils.AppUtils;

public class RecipeInfoActivity extends AppCompatActivity implements RecipeInfoFragment.OnStepClickListener {

    private boolean mTwoPane;
    private Recipe recipe;
    private SharedPreferences sharedPreferences;
    private LinearLayout layoutRecipeInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receive the Parcelable Recipe object from the extras of the intent.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(AppUtils.EXTRAS_RECIPE)) {
                recipe = getIntent().getParcelableExtra(AppUtils.EXTRAS_RECIPE);
            }
        }

        setTitle(recipe.getName());

        setContentView(R.layout.activity_recipe_info);
        layoutRecipeInfo = findViewById(R.id.layout_recipe_info);

        mTwoPane = (findViewById(R.id.fragment_container_detail) != null);
        Log.d("RecipeInfoActivity", "TWO PANE = "+Boolean.toString(mTwoPane));
    }

    public Recipe getRecipe() {
        return recipe;
    }

    @Override
    public void onStepSelected(int position) {
        Step step = recipe.getSteps().get(position);

        // Dealing with known JSON bug.  In some cases the Thumbnail might be confused with the Video URL
        // in that case check the mime type of the thumbnail, and if it is of type video, then swap thumbnail for video
        if (!step.getThumbnailURL().isEmpty()){
            String mimeType = AppUtils.getMimeType(this, Uri.parse(step.getThumbnailURL()));
            if (mimeType.startsWith(AppUtils.MIME_VIDEO)){
                step.swapVideoWithThumb();
            }
        }
        if (!step.getVideoURL().isEmpty()){
            String mimeType = AppUtils.getMimeType(this, Uri.parse(step.getVideoURL()));
            if (mimeType.startsWith(AppUtils.MIME_IMAGE)){
                step.swapVideoWithThumb();
            }
        }

        if (mTwoPane){
            RecipeInfoDetailFragment detailFragment = new RecipeInfoDetailFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("step", step);
            detailFragment.setArguments(bundle);

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_detail, detailFragment)
                    .commit();
        }
        else {
            Intent intent = new Intent(this, RecipeInfoDetailActivity.class);
            intent.putExtra(AppUtils.EXTRAS_STEP, step);
            intent.putExtra(AppUtils.EXTRAS_RECIPE_NAME, recipe.getName());
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recipe_menu, menu);

        // persistence.  Set checked state based on the fetchPopular boolean
        sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, MODE_PRIVATE);
        if ((sharedPreferences.getInt("ID", -1) == recipe.getId())){
            menu.findItem(R.id.mi_action_widget).setIcon(R.drawable.ic_star_white_48dp);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.mi_action_widget){
            boolean isRecipeInWidget = (sharedPreferences.getInt(AppUtils.PREFERENCES_ID, -1) == recipe.getId());

            // If recipe already in widget, remove it
            if (isRecipeInWidget){
                sharedPreferences.edit()
                    .remove(AppUtils.PREFERENCES_ID)
                    .remove(AppUtils.PREFERENCES_WIDGET_TITLE)
                    .remove(AppUtils.PREFERENCES_WIDGET_CONTENT)
                    .apply();

                item.setIcon(R.drawable.ic_star_border_white_48dp);
                Snackbar.make(layoutRecipeInfo, R.string.widget_recipe_removed, Snackbar.LENGTH_SHORT).show();
            }
            // if recipe not in widget, then add it
            else{
                sharedPreferences
                    .edit()
                    .putInt(AppUtils.PREFERENCES_ID, recipe.getId())
                    .putString(AppUtils.PREFERENCES_WIDGET_TITLE, recipe.getName())
                    .putString(AppUtils.PREFERENCES_WIDGET_CONTENT, ingredientsString())
                    .apply();

                item.setIcon(R.drawable.ic_star_white_48dp);
                Snackbar.make(layoutRecipeInfo, R.string.widget_recipe_added, Snackbar.LENGTH_SHORT).show();
            }

            // Put changes on the Widget
            ComponentName provider = new ComponentName(this, BakingWidgetProvider.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] ids = appWidgetManager.getAppWidgetIds(provider);
            BakingWidgetProvider bakingWidgetProvider = new BakingWidgetProvider();
            bakingWidgetProvider.onUpdate(this, appWidgetManager, ids);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Combine dosage and ingredient name to produce a complete line of text to use at widget
     * @return
     */
    private String ingredientsString(){
        StringBuilder result = new StringBuilder();
        for (Ingredient ingredient :  recipe.getIngredients()){
            result.append(ingredient.getDoseStr()).append(" ").append(ingredient.getIngredient()).append("\n");
        }
        return result.toString();
    }
}