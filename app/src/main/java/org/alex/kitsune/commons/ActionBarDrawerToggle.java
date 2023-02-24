package org.alex.kitsune.commons;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.Toolbar;

import java.lang.reflect.Method;

public class ActionBarDrawerToggle implements androidx.drawerlayout.widget.DrawerLayout.DrawerListener {

    /**
     * Allows an implementing Activity to return an {@link ActionBarDrawerToggle.Delegate} to use
     * with ActionBarDrawerToggle.
     */
    public interface DelegateProvider {

        /**
         * @return Delegate to use for ActionBarDrawableToggles, or null if the Activity
         * does not wish to override the default behavior.
         */
        @Nullable
        ActionBarDrawerToggle.Delegate getDrawerToggleDelegate();
    }

    public interface Delegate {

        /**
         * Set the Action Bar's up indicator drawable and content description.
         *
         * @param upDrawable     - Drawable to set as up indicator
         * @param contentDescRes - Content description to set
         */
        void setActionBarUpIndicator(Drawable upDrawable, @StringRes int contentDescRes);

        /**
         * Set the Action Bar's up indicator content description.
         *
         * @param contentDescRes - Content description to set
         */
        void setActionBarDescription(@StringRes int contentDescRes);

        /**
         * Returns the drawable to be set as up button when DrawerToggle is disabled
         */
        Drawable getThemeUpIndicator();

        /**
         * Returns the context of ActionBar
         */
        Context getActionBarThemedContext();

        /**
         * Returns whether navigation icon is visible or not.
         * Used to print warning messages in case developer forgets to set displayHomeAsUp to true
         */
        boolean isNavigationVisible();
    }

    private final ActionBarDrawerToggle.Delegate mActivityImpl;
    private final DrawerLayout mDrawerLayout;

    private DrawerArrowDrawable mSlider;
    private boolean mDrawerSlideAnimationEnabled = true;
    private Drawable mHomeAsUpIndicator;
    boolean mDrawerIndicatorEnabled = true;
    private boolean mHasCustomUpIndicator;
    private final int mOpenDrawerContentDescRes;
    private final int mCloseDrawerContentDescRes;
    // used in toolbar mode when DrawerToggle is disabled
    View.OnClickListener mToolbarNavigationClickListener;
    // If developer does not set displayHomeAsUp, DrawerToggle won't show up.
    // DrawerToggle logs a warning if this case is detected
    private boolean mWarnedForDisplayHomeAsUp = false;

    /**
     * Construct a new ActionBarDrawerToggle.
     *
     * <p>The given {@link Activity} will be linked to the specified {@link DrawerLayout} and
     * its Actionbar's Up button will be set to a custom drawable.
     * <p>This drawable shows a Hamburger icon when drawer is closed and an arrow when drawer
     * is open. It animates between these two states as the drawer opens.</p>
     *
     * <p>String resources must be provided to describe the open/close drawer actions for
     * accessibility services.</p>
     *
     * @param activity                  The Activity hosting the drawer. Should have an ActionBar.
     * @param drawerLayout              The DrawerLayout to link to the given Activity's ActionBar
     * @param openDrawerContentDescRes  A String resource to describe the "open drawer" action
     *                                  for accessibility
     * @param closeDrawerContentDescRes A String resource to describe the "close drawer" action
     *                                  for accessibility
     */
    public ActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout,
                                 @StringRes int openDrawerContentDescRes,
                                 @StringRes int closeDrawerContentDescRes) {
        this(activity, null, drawerLayout, null, openDrawerContentDescRes,
                closeDrawerContentDescRes);
    }

    /**
     * Construct a new ActionBarDrawerToggle with a Toolbar.
     * <p>
     * The given {@link Activity} will be linked to the specified {@link DrawerLayout} and
     * the Toolbar's navigation icon will be set to a custom drawable. Using this constructor
     * will set Toolbar's navigation click listener to toggle the drawer when it is clicked.
     * <p>
     * This drawable shows a Hamburger icon when drawer is closed and an arrow when drawer
     * is open. It animates between these two states as the drawer opens.
     * <p>
     * String resources must be provided to describe the open/close drawer actions for
     * accessibility services.
     * <p>
     * Please use {@link #ActionBarDrawerToggle(Activity, DrawerLayout, int, int)} if you are
     * setting the Toolbar as the ActionBar of your activity.
     *
     * @param activity                  The Activity hosting the drawer.
     * @param toolbar                   The toolbar to use if you have an independent Toolbar.
     * @param drawerLayout              The DrawerLayout to link to the given Activity's ActionBar
     * @param openDrawerContentDescRes  A String resource to describe the "open drawer" action
     *                                  for accessibility
     * @param closeDrawerContentDescRes A String resource to describe the "close drawer" action
     *                                  for accessibility
     */
    public ActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout,
                                 Toolbar toolbar, @StringRes int openDrawerContentDescRes,
                                 @StringRes int closeDrawerContentDescRes) {
        this(activity, toolbar, drawerLayout, null, openDrawerContentDescRes,
                closeDrawerContentDescRes);
    }

    /**
     * In the future, we can make this constructor public if we want to let developers customize
     * the
     * animation.
     */
    ActionBarDrawerToggle(Activity activity, Toolbar toolbar, DrawerLayout drawerLayout,
                          DrawerArrowDrawable slider, @StringRes int openDrawerContentDescRes,
                          @StringRes int closeDrawerContentDescRes) {
        if (toolbar != null) {
            mActivityImpl = new ActionBarDrawerToggle.ToolbarCompatDelegate(toolbar);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDrawerIndicatorEnabled) {
                        toggle();
                    } else if (mToolbarNavigationClickListener != null) {
                        mToolbarNavigationClickListener.onClick(v);
                    }
                }
            });
        } else if (activity instanceof ActionBarDrawerToggle.DelegateProvider) { // Allow the Activity to provide an impl
            mActivityImpl = ((ActionBarDrawerToggle.DelegateProvider) activity).getDrawerToggleDelegate();
        } else {
            mActivityImpl = new ActionBarDrawerToggle.FrameworkActionBarDelegate(activity);
        }

        mDrawerLayout = drawerLayout;
        mOpenDrawerContentDescRes = openDrawerContentDescRes;
        mCloseDrawerContentDescRes = closeDrawerContentDescRes;
        if (slider == null) {
            mSlider = new DrawerArrowDrawable(mActivityImpl.getActionBarThemedContext());
        } else {
            mSlider = slider;
        }

        mHomeAsUpIndicator = getThemeUpIndicator();
    }

    public void syncState() {
        if (mDrawerLayout.isDrawerOpen()) {
            setPosition(1);
        } else {
            setPosition(0);
        }
        if (mDrawerIndicatorEnabled) {
            setActionBarUpIndicator(mSlider,
                    mDrawerLayout.isDrawerOpen() ?
                            mCloseDrawerContentDescRes : mOpenDrawerContentDescRes);
        }
    }

    /**
     * This method should always be called by your <code>Activity</code>'s
     * {@link Activity#onConfigurationChanged(android.content.res.Configuration)
     * onConfigurationChanged}
     * method.
     *
     * @param newConfig The new configuration
     */
    public void onConfigurationChanged(Configuration newConfig) {
        // Reload drawables that can change with configuration
        if (!mHasCustomUpIndicator) {
            mHomeAsUpIndicator = getThemeUpIndicator();
        }
        syncState();
    }

    /**
     * This method should be called by your <code>Activity</code>'s
     * {@link Activity#onOptionsItemSelected(android.view.MenuItem) onOptionsItemSelected} method.
     * If it returns true, your <code>onOptionsItemSelected</code> method should return true and
     * skip further processing.
     *
     * @param item the MenuItem instance representing the selected menu item
     * @return true if the event was handled and further processing should not occur
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null && item.getItemId() == android.R.id.home && mDrawerIndicatorEnabled) {
            toggle();
            return true;
        }
        return false;
    }

    void toggle() {
        if(mDrawerLayout.isDrawerOpen()){
            mDrawerLayout.closeDrawer();
        }else{
            mDrawerLayout.openDrawer();
        }
    }

    /**
     * Set the up indicator to display when the drawer indicator is not
     * enabled.
     * <p>
     * If you pass <code>null</code> to this method, the default drawable from
     * the theme will be used.
     *
     * @param indicator A drawable to use for the up indicator, or null to use
     *                  the theme's default
     * @see #setDrawerIndicatorEnabled(boolean)
     */
    public void setHomeAsUpIndicator(Drawable indicator) {
        if (indicator == null) {
            mHomeAsUpIndicator = getThemeUpIndicator();
            mHasCustomUpIndicator = false;
        } else {
            mHomeAsUpIndicator = indicator;
            mHasCustomUpIndicator = true;
        }

        if (!mDrawerIndicatorEnabled) {
            setActionBarUpIndicator(mHomeAsUpIndicator, 0);
        }
    }

    /**
     * Set the up indicator to display when the drawer indicator is not
     * enabled.
     * <p>
     * If you pass 0 to this method, the default drawable from the theme will
     * be used.
     *
     * @param resId Resource ID of a drawable to use for the up indicator, or 0
     *              to use the theme's default
     * @see #setDrawerIndicatorEnabled(boolean)
     */
    public void setHomeAsUpIndicator(int resId) {
        Drawable indicator = null;
        if (resId != 0) {
            indicator = mDrawerLayout.getResources().getDrawable(resId);
        }
        setHomeAsUpIndicator(indicator);
    }

    /**
     * @return true if the enhanced drawer indicator is enabled, false otherwise
     * @see #setDrawerIndicatorEnabled(boolean)
     */
    public boolean isDrawerIndicatorEnabled() {
        return mDrawerIndicatorEnabled;
    }

    /**
     * Enable or disable the drawer indicator. The indicator defaults to enabled.
     *
     * <p>When the indicator is disabled, the <code>ActionBar</code> will revert to displaying
     * the home-as-up indicator provided by the <code>Activity</code>'s theme in the
     * <code>android.R.attr.homeAsUpIndicator</code> attribute instead of the animated
     * drawer glyph.</p>
     *
     * @param enable true to enable, false to disable
     */
    public void setDrawerIndicatorEnabled(boolean enable) {
        if (enable != mDrawerIndicatorEnabled) {
            if (enable) {
                setActionBarUpIndicator(mSlider,
                        mDrawerLayout.isDrawerOpen() ?
                                mCloseDrawerContentDescRes : mOpenDrawerContentDescRes);
            } else {
                setActionBarUpIndicator(mHomeAsUpIndicator, 0);
            }
            mDrawerIndicatorEnabled = enable;
        }
    }

    /**
     * @return DrawerArrowDrawable that is currently shown by the ActionBarDrawerToggle.
     */
    @NonNull
    public DrawerArrowDrawable getDrawerArrowDrawable() {
        return mSlider;
    }

    /**
     * Sets the DrawerArrowDrawable that should be shown by this ActionBarDrawerToggle.
     *
     * @param drawable DrawerArrowDrawable that should be shown by this ActionBarDrawerToggle
     */
    public void setDrawerArrowDrawable(@NonNull DrawerArrowDrawable drawable) {
        mSlider = drawable;
        syncState();
    }

    /**
     * Specifies whether the drawer arrow should animate when the drawer position changes.
     *
     * @param enabled if this is {@code true} then the animation will run, else it will be skipped
     */
    public void setDrawerSlideAnimationEnabled(boolean enabled) {
        mDrawerSlideAnimationEnabled = enabled;
        if (!enabled) {
            setPosition(0);
        }
    }

    /**
     * @return whether the drawer slide animation is enabled
     */
    public boolean isDrawerSlideAnimationEnabled() {
        return mDrawerSlideAnimationEnabled;
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        if (mDrawerSlideAnimationEnabled) {
            setPosition(Math.min(1f, Math.max(0, slideOffset)));
        } else {
            setPosition(0); // disable animation.
        }
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        setPosition(1);
        if (mDrawerIndicatorEnabled) {
            setActionBarDescription(mCloseDrawerContentDescRes);
        }
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        setPosition(0);
        if (mDrawerIndicatorEnabled) {
            setActionBarDescription(mOpenDrawerContentDescRes);
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    /**
     * Returns the fallback listener for Navigation icon click events.
     *
     * @return The click listener which receives Navigation click events from Toolbar when
     * drawer indicator is disabled.
     * @see #setToolbarNavigationClickListener(android.view.View.OnClickListener)
     * @see #setDrawerIndicatorEnabled(boolean)
     * @see #isDrawerIndicatorEnabled()
     */
    public View.OnClickListener getToolbarNavigationClickListener() {
        return mToolbarNavigationClickListener;
    }

    /**
     * When DrawerToggle is constructed with a Toolbar, it sets the click listener on
     * the Navigation icon. If you want to listen for clicks on the Navigation icon when
     * DrawerToggle is disabled ({@link #setDrawerIndicatorEnabled(boolean)}, you should call this
     * method with your listener and DrawerToggle will forward click events to that listener
     * when drawer indicator is disabled.
     *
     * @see #setDrawerIndicatorEnabled(boolean)
     */
    public void setToolbarNavigationClickListener(
            View.OnClickListener onToolbarNavigationClickListener) {
        mToolbarNavigationClickListener = onToolbarNavigationClickListener;
    }

    void setActionBarUpIndicator(Drawable upDrawable, int contentDescRes) {
        if (!mWarnedForDisplayHomeAsUp && !mActivityImpl.isNavigationVisible()) {
            Log.w("ActionBarDrawerToggle", "DrawerToggle may not show up because NavigationIcon"
                    + " is not visible. You may need to call "
                    + "actionbar.setDisplayHomeAsUpEnabled(true);");
            mWarnedForDisplayHomeAsUp = true;
        }
        mActivityImpl.setActionBarUpIndicator(upDrawable, contentDescRes);
    }

    void setActionBarDescription(int contentDescRes) {
        mActivityImpl.setActionBarDescription(contentDescRes);
    }

    Drawable getThemeUpIndicator() {
        return mActivityImpl.getThemeUpIndicator();
    }

    private void setPosition(float position) {
        if (position == 1f) {
            mSlider.setVerticalMirror(true);
        } else if (position == 0f) {
            mSlider.setVerticalMirror(false);
        }
        mSlider.setProgress(position);
    }

    private static class FrameworkActionBarDelegate implements ActionBarDrawerToggle.Delegate {
        private final Activity mActivity;
        private ActionBarDrawerToggleHoneycomb.SetIndicatorInfo mSetIndicatorInfo;

        FrameworkActionBarDelegate(Activity activity) {
            mActivity = activity;
        }

        @Override
        public Drawable getThemeUpIndicator() {
            if (Build.VERSION.SDK_INT >= 18) {
                final TypedArray a = getActionBarThemedContext().obtainStyledAttributes(null,
                        new int[] {android.R.attr.homeAsUpIndicator},
                        android.R.attr.actionBarStyle, 0);
                final Drawable result = a.getDrawable(0);
                a.recycle();
                return result;
            }
            return ActionBarDrawerToggleHoneycomb.getThemeUpIndicator(mActivity);
        }

        @Override
        public Context getActionBarThemedContext() {
            final ActionBar actionBar = mActivity.getActionBar();
            if (actionBar != null) {
                return actionBar.getThemedContext();
            }
            return mActivity;
        }

        @Override
        public boolean isNavigationVisible() {
            final ActionBar actionBar = mActivity.getActionBar();
            return actionBar != null
                    && (actionBar.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0;
        }

        @Override
        public void setActionBarUpIndicator(Drawable themeImage, int contentDescRes) {
            final ActionBar actionBar = mActivity.getActionBar();
            if (actionBar != null) {
                if (Build.VERSION.SDK_INT >= 18) {
                    ActionBarDrawerToggle.FrameworkActionBarDelegate.Api18Impl.setHomeAsUpIndicator(actionBar, themeImage);
                    ActionBarDrawerToggle.FrameworkActionBarDelegate.Api18Impl.setHomeActionContentDescription(actionBar, contentDescRes);
                } else {
                    actionBar.setDisplayShowHomeEnabled(true);
                    mSetIndicatorInfo = ActionBarDrawerToggleHoneycomb.setActionBarUpIndicator(
                            mActivity, themeImage, contentDescRes);
                    actionBar.setDisplayShowHomeEnabled(false);
                }
            }
        }

        @Override
        public void setActionBarDescription(int contentDescRes) {
            if (Build.VERSION.SDK_INT >= 18) {
                final ActionBar actionBar = mActivity.getActionBar();
                if (actionBar != null) {
                    ActionBarDrawerToggle.FrameworkActionBarDelegate.Api18Impl.setHomeActionContentDescription(actionBar, contentDescRes);
                }
            } else {
                mSetIndicatorInfo = ActionBarDrawerToggleHoneycomb.setActionBarDescription(
                        mSetIndicatorInfo, mActivity, contentDescRes);
            }
        }

        @RequiresApi(18)
        static class Api18Impl {
            private Api18Impl() {
                // This class is not instantiable.
            }

            @DoNotInline
            static void setHomeActionContentDescription(ActionBar actionBar, int resId) {
                actionBar.setHomeActionContentDescription(resId);
            }

            @DoNotInline
            static void setHomeAsUpIndicator(ActionBar actionBar, Drawable indicator) {
                actionBar.setHomeAsUpIndicator(indicator);
            }

        }
    }

    /**
     * Used when DrawerToggle is initialized with a Toolbar
     */
    static class ToolbarCompatDelegate implements ActionBarDrawerToggle.Delegate {

        final Toolbar mToolbar;
        final Drawable mDefaultUpIndicator;
        final CharSequence mDefaultContentDescription;

        ToolbarCompatDelegate(Toolbar toolbar) {
            mToolbar = toolbar;
            mDefaultUpIndicator = toolbar.getNavigationIcon();
            mDefaultContentDescription = toolbar.getNavigationContentDescription();
        }

        @Override
        public void setActionBarUpIndicator(Drawable upDrawable, @StringRes int contentDescRes) {
            mToolbar.setNavigationIcon(upDrawable);
            setActionBarDescription(contentDescRes);
        }

        @Override
        public void setActionBarDescription(@StringRes int contentDescRes) {
            if (contentDescRes == 0) {
                mToolbar.setNavigationContentDescription(mDefaultContentDescription);
            } else {
                mToolbar.setNavigationContentDescription(contentDescRes);
            }
        }

        @Override
        public Drawable getThemeUpIndicator() {
            return mDefaultUpIndicator;
        }

        @Override
        public Context getActionBarThemedContext() {
            return mToolbar.getContext();
        }

        @Override
        public boolean isNavigationVisible() {
            return true;
        }
    }

    class ActionBarDrawerToggleHoneycomb {
        private static final String TAG = "ActionBarDrawerToggleHC";

        private static final int[] THEME_ATTRS = new int[] {
                android.R.attr.homeAsUpIndicator
        };

        public static ActionBarDrawerToggleHoneycomb.SetIndicatorInfo setActionBarUpIndicator(Activity activity, Drawable drawable, int contentDescRes) {
            ActionBarDrawerToggleHoneycomb.SetIndicatorInfo info = new ActionBarDrawerToggleHoneycomb.SetIndicatorInfo(activity);
            if (info.setHomeAsUpIndicator != null) {
                try {
                    final ActionBar actionBar = activity.getActionBar();
                    info.setHomeAsUpIndicator.invoke(actionBar, drawable);
                    info.setHomeActionContentDescription.invoke(actionBar, contentDescRes);
                } catch (Exception e) {
                    Log.w(TAG, "Couldn't set home-as-up indicator via JB-MR2 API", e);
                }
            } else if (info.upIndicatorView != null) {
                info.upIndicatorView.setImageDrawable(drawable);
            } else {
                Log.w(TAG, "Couldn't set home-as-up indicator");
            }
            return info;
        }

        public static ActionBarDrawerToggleHoneycomb.SetIndicatorInfo setActionBarDescription(ActionBarDrawerToggleHoneycomb.SetIndicatorInfo info, Activity activity, int contentDescRes) {
            if (info == null) {
                info = new ActionBarDrawerToggleHoneycomb.SetIndicatorInfo(activity);
            }
            if (info.setHomeAsUpIndicator != null) {
                try {
                    final ActionBar actionBar = activity.getActionBar();
                    info.setHomeActionContentDescription.invoke(actionBar, contentDescRes);
                    if (Build.VERSION.SDK_INT <= 19) {
                        // For API 19 and earlier, we need to manually force the
                        // action bar to generate a new content description.
                        actionBar.setSubtitle(actionBar.getSubtitle());
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Couldn't set content description via JB-MR2 API", e);
                }
            }
            return info;
        }

        public static Drawable getThemeUpIndicator(Activity activity) {
            final TypedArray a = activity.obtainStyledAttributes(THEME_ATTRS);
            final Drawable result = a.getDrawable(0);
            a.recycle();
            return result;
        }

        static class SetIndicatorInfo {
            public Method setHomeAsUpIndicator;
            public Method setHomeActionContentDescription;
            public ImageView upIndicatorView;

            SetIndicatorInfo(Activity activity) {
                try {
                    setHomeAsUpIndicator = ActionBar.class.getDeclaredMethod("setHomeAsUpIndicator",
                            Drawable.class);
                    setHomeActionContentDescription = ActionBar.class.getDeclaredMethod(
                            "setHomeActionContentDescription", Integer.TYPE);

                    // If we got the method we won't need the stuff below.
                    return;
                } catch (NoSuchMethodException e) {
                    // Oh well. We'll use the other mechanism below instead.
                }

                final View home = activity.findViewById(android.R.id.home);
                if (home == null) {
                    // Action bar doesn't have a known configuration, an OEM messed with things.
                    return;
                }

                final ViewGroup parent = (ViewGroup) home.getParent();
                final int childCount = parent.getChildCount();
                if (childCount != 2) {
                    // No idea which one will be the right one, an OEM messed with things.
                    return;
                }

                final View first = parent.getChildAt(0);
                final View second = parent.getChildAt(1);
                final View up = first.getId() == android.R.id.home ? second : first;

                if (up instanceof ImageView) {
                    // Jackpot! (Probably...)
                    upIndicatorView = (ImageView) up;
                }
            }
        }

        private ActionBarDrawerToggleHoneycomb() {
        }
    }
}

