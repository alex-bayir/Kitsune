package com.alex.listitemview.util.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuPopupHelper;
import com.alex.listitemview.R;
import com.alex.listitemview.util.ViewUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * View to represent action items on the right side of list item.
 *
 * @author Lucas Urbas
 */
@SuppressWarnings("RestrictedApi")
public class MenuView extends LinearLayout implements Checkable {

    private int mMenuResId = -1;

    private MenuBuilder mMenuBuilder;

    private SupportMenuInflater mMenuInflater;

    private MenuPopupHelper mMenuPopupHelper;

    private MenuBuilder.Callback mMenuCallback;

    private int mActionIconColor;

    private ColorStateList mActionIconColorList;

    private int mOverflowIconColor;

    //all menu items
    private List<MenuItemImpl> mMenuItems;

    //items that are currently presented as actions
    private List<MenuItemImpl> mActionItems = new ArrayList<>();

    private boolean mChecked;

    private boolean mHasOverflow = false;


    public MenuView(Context context) {
        this(context, null);
    }

    public MenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        int color = ViewUtils.getDefaultColor(getContext());
        mActionIconColor = color;
        mOverflowIconColor = color;
    }

    public void setActionIconColor(final int actionColor) {
        this.mActionIconColor = actionColor;
        refreshColors();
    }

    public void setActionIconColorList(final ColorStateList colorStateList) {
        this.mActionIconColorList = colorStateList;
        for (int i = 0; i < getChildCount(); i++) {
            ViewUtils.setIconColor(((ImageView) getChildAt(i)), mActionIconColorList);
        }
    }

    public void setOverflowColor(final int overflowColor) {
        this.mOverflowIconColor = overflowColor;
        refreshColors();
    }

    private void refreshColors() {
        for (int i = 0; i < getChildCount(); i++) {
            ViewUtils.setIconColor(((ImageView) getChildAt(i)), mActionIconColor);
            if (mHasOverflow && i == getChildCount() - 1) {
                ViewUtils.setIconColor(((ImageView) getChildAt(i)), mOverflowIconColor);
            }
        }
    }

    /**
     * Set the callback that will be called when menu
     * items a selected.
     */
    public void setMenuCallback(final MenuBuilder.Callback menuCallback) {
        this.mMenuCallback = menuCallback;
    }


    /**
     * Resets the the view to fit into a new available width.
     * <p>
     * This clears and then re-inflates the menu items, removes all of its associated action
     * views, and re-creates the menu and action items to fit in the new width.
     * </p>
     *
     * @param menuBuilder   builder containing the items to display
     * @param menuItemsRoom the number of the menu items to show. If
     *                      there is room, menu items that are flagged with
     *                      android:showAsAction="ifRoom" or android:showAsAction="always"
     *                      will show as actions.
     */
    public void reset(@NonNull final MenuBuilder menuBuilder, int menuItemsRoom) {

        //clean view and re-inflate
        removeAllViews();

        mMenuBuilder = menuBuilder;
        mMenuPopupHelper = new MenuPopupHelper(getContext(), mMenuBuilder, this);

        mActionItems = new ArrayList<>();
        mMenuItems = new ArrayList<>();

        mMenuItems = mMenuBuilder.getActionItems();
        mMenuItems.addAll(mMenuBuilder.getNonActionItems());

        Collections.sort(mMenuItems, new Comparator<MenuItemImpl>() {
            @Override
            public int compare(MenuItemImpl lhs, MenuItemImpl rhs) {
                return ((Integer) lhs.getOrder()).compareTo(rhs.getOrder());
            }
        });

        List<MenuItemImpl> localActionItems = filter(mMenuItems, new MenuItemImplPredicate() {
            @Override
            public boolean apply(MenuItemImpl menuItem) {
                return menuItem.getIcon() != null && (menuItem.requiresActionButton()
                        || menuItem.requestsActionButton());
            }
        });

        //determine if to show overflow menu
        boolean addOverflowAtTheEnd = false;
        if (((localActionItems.size() < mMenuItems.size())
                || menuItemsRoom < localActionItems.size())) {
            addOverflowAtTheEnd = true;
            menuItemsRoom--;
        }

        ArrayList<Integer> actionItemsIds = new ArrayList<>();
        if (menuItemsRoom > 0) {
            for (int i = 0; i < localActionItems.size(); i++) {

                final MenuItemImpl menuItem = localActionItems.get(i);
                if (menuItem.getIcon() != null) {

                    ImageView action = createActionView();
                    action.setImageDrawable(menuItem.getIcon());
                    if (mActionIconColorList != null) {
                        ViewUtils.setIconColor(action, mActionIconColorList);
                    } else {
                        ViewUtils.setIconColor(action, mActionIconColor);
                    }
                    addView(action);
                    mActionItems.add(menuItem);
                    actionItemsIds.add(menuItem.getItemId());
                    menuItem.setActionView(action);

                    if (mMenuCallback != null) {
                        action.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mMenuCallback.onMenuItemSelected(mMenuBuilder, menuItem);
                            }
                        });
                    } else {
                        action.setBackground(null);
                    }

                    final int[] stateSet = {android.R.attr.state_checked * (isChecked() ? 1 : -1)};
                    action.setImageState(stateSet, true);
                    action.getDrawable().jumpToCurrentState();

                    menuItemsRoom--;
                    if (menuItemsRoom == 0) {
                        break;
                    }
                }
            }
        }

        mHasOverflow = addOverflowAtTheEnd;
        if (addOverflowAtTheEnd) {

            ImageView overflowAction = getOverflowActionView();
            overflowAction.setImageResource(R.drawable.vd_more_vert_black_24dp);
            ViewUtils.setIconColor(overflowAction, mOverflowIconColor);
            addView(overflowAction);

            overflowAction.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMenuPopupHelper.show();
                }
            });

            mMenuBuilder.setCallback(mMenuCallback);
        }

        //remove all menu items that will be shown as icons (the action items) from the overflow menu
        for (int id : actionItemsIds) {
            mMenuBuilder.removeItem(id);
        }
        actionItemsIds = null;
    }

    /**
     * Resets the the view to fit into a new available width.
     * <p>
     * This clears and then re-inflates the menu items, removes all of its associated action
     * views, and re-creates the menu and action items to fit in the new width.
     * </p>
     *
     * @param menuItemsRoom the number of the menu items to show. If
     *                      there is room, menu items that are flagged with
     *                      android:showAsAction="ifRoom" or android:showAsAction="always"
     *                      will show as actions.
     */
    public void reset(final int menuResId, int menuItemsRoom) {
        mMenuResId = menuResId;

        //clean view and re-inflate
        removeAllViews();

        if (mMenuResId == -1) {
            return;
        }

        final MenuBuilder builder = new MenuBuilder(getContext());
        getMenuInflater().inflate(mMenuResId, builder);
        reset(builder, menuItemsRoom);
    }

    private ImageView createActionView() {
        return (ImageView) LayoutInflater.from(getContext())
                .inflate(R.layout.liv_action_item_layout, this, false);
    }

    private ImageView getOverflowActionView() {
        return (ImageView) LayoutInflater.from(getContext())
                .inflate(R.layout.liv_action_item_overflow_layout, this, false);
    }

    public List<MenuItemImpl> getMenuItems() {
        return mMenuItems;
    }

    private interface MenuItemImplPredicate {

        boolean apply(MenuItemImpl menuItem);
    }

    private List<MenuItemImpl> filter(List<MenuItemImpl> target, MenuItemImplPredicate predicate) {
        List<MenuItemImpl> result = new ArrayList<>();
        for (MenuItemImpl element : target) {
            if (predicate.apply(element)) {
                result.add(element);
            }
        }
        return result;
    }

    private MenuInflater getMenuInflater() {
        if (mMenuInflater == null) {
            mMenuInflater = new SupportMenuInflater(getContext());
        }
        return mMenuInflater;
    }

    /**
     * Set a checked state of the item.
     *
     * @param checked a new item checked state
     */
    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            for(int i = 0; i < getChildCount(); i++){
                if (mHasOverflow && i == getChildCount() - 1) {
                    continue;
                }
                ImageView child = (ImageView) getChildAt(i);
                final int[] stateSet = {android.R.attr.state_checked * (isChecked() ? 1 : -1)};
                child.setImageState(stateSet, true);
            }
        }
    }

    /**
     * Check if item is checked.
     *
     * @return if item is checked
     */
    @Override
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Change the checked state of the item to the opposite.
     */
    @Override
    public void toggle() {
        setChecked(!mChecked);
    }
}

